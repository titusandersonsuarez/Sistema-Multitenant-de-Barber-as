package com.barberhub;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class BookingTest extends ApiTestSupport {

    private List<String> slots(Shop shop, long serviceId, String date) throws Exception {
        JsonNode body = call(GET("/api/public/" + shop.slug() + "/availability?barberId=" + shop.barberId()
                + "&serviceId=" + serviceId + "&date=" + date), null, null).body();
        List<String> out = new ArrayList<>();
        body.get("slots").forEach(n -> out.add(n.asText()));
        return out;
    }

    @Test
    void laDisponibilidadRespetaElHorarioYLaDuracion() throws Exception {
        Shop shop = registerShop();
        // Martes 9:00–19:00, corte de 30 min cada 15 min: el último turno empieza 18:30.
        List<String> tuesday = slots(shop, shop.serviceId30(), "2026-10-20");
        assertThat(tuesday).startsWith("09:00", "09:15").endsWith("18:30").hasSize(39);
        // El domingo no trabaja.
        assertThat(slots(shop, shop.serviceId30(), "2026-10-25")).isEmpty();
        // Hoy lunes a las 8:00: todavía están todos los turnos de hoy.
        assertThat(slots(shop, shop.serviceId30(), "2026-10-19")).first().isEqualTo("09:00");
        // Ayer no.
        assertThat(slots(shop, shop.serviceId30(), "2026-10-18")).isEmpty();
    }

    @Test
    void reservarOcupaElHorarioYNoSePuedeReservarDosVeces() throws Exception {
        Shop shop = registerShop();
        Response first = book(shop, shop.barberId(), shop.serviceId45(), "2026-10-20T10:00:00");
        assertThat(first.status()).isEqualTo(201);
        assertThat(first.body().get("code").asText()).hasSize(8);
        assertThat(first.body().get("endAt").asText()).isEqualTo("2026-10-20T10:45:00");
        assertThat(first.body().get("priceCop").asInt()).isEqualTo(35_000);

        // 10:00–10:45 ocupado: un corte de 30 min no cabe entre 09:45 y 10:30.
        List<String> after = slots(shop, shop.serviceId30(), "2026-10-20");
        assertThat(after).contains("09:30", "10:45").doesNotContain("09:45", "10:00", "10:15", "10:30");

        assertThat(book(shop, shop.barberId(), shop.serviceId30(), "2026-10-20T10:00:00").status()).isEqualTo(409);
        assertThat(book(shop, shop.barberId(), shop.serviceId30(), "2026-10-20T10:30:00").status()).isEqualTo(409);
    }

    @Test
    void cancelarLiberaElHorario() throws Exception {
        Shop shop = registerShop();
        String code = book(shop, shop.barberId(), shop.serviceId30(), "2026-10-21T15:00:00").body().get("code").asText();
        assertThat(slots(shop, shop.serviceId30(), "2026-10-21")).doesNotContain("15:00");

        Response cancelled = call(POST("/api/public/" + shop.slug() + "/appointments/" + code.toLowerCase() + "/cancel"), null, null);
        assertThat(cancelled.status()).isEqualTo(200);
        assertThat(cancelled.body().get("status").asText()).isEqualTo("CANCELLED");
        assertThat(slots(shop, shop.serviceId30(), "2026-10-21")).contains("15:00");

        // No se cancela dos veces.
        assertThat(call(POST("/api/public/" + shop.slug() + "/appointments/" + code + "/cancel"), null, null).status()).isEqualTo(409);
        // Y el horario se puede volver a reservar.
        assertThat(book(shop, shop.barberId(), shop.serviceId30(), "2026-10-21T15:00:00").status()).isEqualTo(201);
    }

    @Test
    void rechazaHorariosInvalidos() throws Exception {
        Shop shop = registerShop();
        assertThat(book(shop, shop.barberId(), shop.serviceId30(), "2026-10-19T07:00:00").status()).isEqualTo(400); // pasado
        assertThat(book(shop, shop.barberId(), shop.serviceId30(), "2026-10-20T07:00:00").status()).isEqualTo(409); // antes de abrir
        assertThat(book(shop, shop.barberId(), shop.serviceId30(), "2026-10-20T18:45:00").status()).isEqualTo(409); // no alcanza
        assertThat(book(shop, shop.barberId(), shop.serviceId30(), "2026-10-25T10:00:00").status()).isEqualTo(409); // domingo
        assertThat(book(shop, shop.barberId(), shop.serviceId30(), "2027-03-01T10:00:00").status()).isEqualTo(400); // muy lejos
        Response invalid = call(POST("/api/public/" + shop.slug() + "/appointments"), null, Map.of(
                "barberId", shop.barberId(), "serviceId", shop.serviceId30(), "startAt", "2026-10-20T10:00:00",
                "customerName", "", "customerPhone", "abc"));
        assertThat(invalid.status()).isEqualTo(400);
        assertThat(invalid.body().get("campos").has("customerPhone")).isTrue();
    }

    @Test
    void dosClientesAlMismoTiempoNoObtienenElMismoTurno() throws Exception {
        Shop shop = registerShop();
        int clients = 8;
        ExecutorService pool = Executors.newFixedThreadPool(clients);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < clients; i++) {
            Callable<Integer> task = () -> {
                start.await();
                return book(shop, shop.barberId(), shop.serviceId30(), "2026-10-22T11:00:00").status();
            };
            results.add(pool.submit(task));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : results) statuses.add(f.get());
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(clients - 1);
    }

    @Test
    void elDuenoGestionaLaAgendaYVeElResumen() throws Exception {
        Shop shop = registerShop();
        book(shop, shop.barberId(), shop.serviceId30(), "2026-10-20T09:00:00");
        book(shop, shop.barberId(), shop.serviceId45(), "2026-10-20T10:00:00");
        Response walkIn = call(POST("/api/admin/appointments"), shop.token(), Map.of(
                "barberId", shop.barberId(), "serviceId", shop.serviceId30(), "startAt", "2026-10-20T12:00:00",
                "customerName", "Cliente de mostrador", "customerPhone", "3009998877"));
        assertThat(walkIn.status()).isEqualTo(201);

        JsonNode agenda = call(GET("/api/admin/appointments?from=2026-10-20"), shop.token(), null).body();
        assertThat(agenda).hasSize(3);
        assertThat(agenda.get(0).get("serviceName").asText()).isEqualTo("Corte clásico");

        for (JsonNode item : agenda) {
            String status = item.get("customerName").asText().equals("Cliente de mostrador") ? "NO_SHOW" : "COMPLETED";
            call(PATCH("/api/admin/appointments/" + item.get("id").asLong() + "/status"), shop.token(), Map.of("status", status));
        }

        JsonNode dash = call(GET("/api/admin/dashboard?from=2026-10-19&to=2026-10-25"), shop.token(), null).body();
        assertThat(dash.get("completed").asLong()).isEqualTo(2);
        assertThat(dash.get("noShow").asLong()).isEqualTo(1);
        assertThat(dash.get("revenueCop").asLong()).isEqualTo(60_000);
        assertThat(dash.get("byService").get(0).get("name").asText()).isEqualTo("Corte + barba");
    }

    @Test
    void unBarberoSoloVeSuPropiaAgenda() throws Exception {
        Shop shop = registerShop();
        long otherBarber = call(POST("/api/admin/barbers"), shop.token(), Map.of("name", "Carlos")).body().get("id").asLong();
        call(PUT("/api/admin/barbers/" + otherBarber + "/hours"), shop.token(), Map.of("blocks", List.of(
                Map.of("dayOfWeek", 2, "start", "10:00", "end", "16:00"))));
        assertThat(call(POST("/api/admin/barbers/" + otherBarber + "/account"), shop.token(),
                Map.of("email", "carlos@" + shop.slug() + ".co", "password", "clave-carlos-1")).status()).isEqualTo(201);

        book(shop, shop.barberId(), shop.serviceId30(), "2026-10-20T10:00:00");
        book(shop, otherBarber, shop.serviceId30(), "2026-10-20T10:00:00");

        String carlos = call(POST("/api/auth/login"), null, Map.of(
                "slug", shop.slug(), "email", "carlos@" + shop.slug() + ".co", "password", "clave-carlos-1"))
                .body().get("token").asText();

        JsonNode agenda = call(GET("/api/admin/appointments?from=2026-10-20"), carlos, null).body();
        assertThat(agenda).hasSize(1);
        assertThat(agenda.get(0).get("barberName").asText()).isEqualTo("Carlos");

        assertThat(call(GET("/api/admin/appointments?from=2026-10-20&barberId=" + shop.barberId()), carlos, null).status())
                .isEqualTo(403);
        // Un barbero no puede cambiar servicios ni precios.
        assertThat(call(POST("/api/admin/services"), carlos, Map.of("name", "X", "durationMinutes", 10, "priceCop", 1)).status())
                .isEqualTo(403);
    }

    @Test
    void elRegistroValidaLaDireccion() throws Exception {
        Shop shop = registerShop();
        Response dup = call(POST("/api/auth/register"), null, Map.of("shopName", "Otra", "slug", shop.slug(),
                "ownerName", "X", "email", "x@x.co", "password", "clave-segura-1"));
        assertThat(dup.status()).isEqualTo(409);
        Response reserved = call(POST("/api/auth/register"), null, Map.of("shopName", "Otra", "slug", "admin",
                "ownerName", "X", "email", "x@x.co", "password", "clave-segura-1"));
        assertThat(reserved.status()).isEqualTo(409);
        Response longPwd = call(POST("/api/auth/register"), null, Map.of("shopName", "Otra", "slug", "pwd-larga-" + shop.slug(),
                "ownerName", "X", "email", "x@x.co", "password", "ñ".repeat(40)));
        assertThat(longPwd.status()).isEqualTo(400);
        assertThat(call(PUT("/api/admin/barbers/" + shop.barberId() + "/hours"), shop.token(),
                Map.of("blocks", java.util.Arrays.asList((Object) null))).status()).isEqualTo(400);
        Response bad = call(POST("/api/auth/register"), null, Map.of("shopName", "Otra", "slug", "Con Espacios",
                "ownerName", "X", "email", "x@x.co", "password", "clave-segura-1"));
        assertThat(bad.status()).isEqualTo(400);
    }
}
