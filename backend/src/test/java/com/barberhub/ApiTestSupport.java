package com.barberhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestInfra.class)
@TestPropertySource(properties = {
        "barberhub.superadmin.email=root@barberhub.test",
        "barberhub.superadmin.password=super-secret-123",
        "barberhub.jwt.secret=test-secret-solo-para-pruebas-0123456789abcdef",
        "barberhub.rate-limit.enabled=false"
})
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    /** Barbería recién registrada: token del dueño + ids útiles. */
    public record Shop(String slug, String token, long barberId, long serviceId30, long serviceId45) {
    }

    public record Response(int status, JsonNode body) {
    }

    protected Shop registerShop() throws Exception {
        String slug = "barberia-" + UUID.randomUUID().toString().substring(0, 8);
        Response r = call(post("/api/auth/register"), null, Map.of(
                "shopName", "Barbería " + slug, "slug", slug, "ownerName", "Dueño " + slug,
                "email", "owner@" + slug + ".co", "password", "clave-segura-1"));
        if (r.status() != 201) {
            throw new IllegalStateException("Registro falló: " + r);
        }
        String token = r.body().get("token").asText();
        long barberId = r.body().get("user").get("barberId").asLong();
        JsonNode services = call(get("/api/admin/services"), token, null).body();
        long s30 = 0;
        long s45 = 0;
        for (JsonNode s : services) {
            if (s.get("durationMinutes").asInt() == 30) s30 = s.get("id").asLong();
            if (s.get("durationMinutes").asInt() == 45) s45 = s.get("id").asLong();
        }
        return new Shop(slug, token, barberId, s30, s45);
    }

    protected Response call(MockHttpServletRequestBuilder req, String token, Object body) throws Exception {
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        }
        MvcResult res = mvc.perform(req).andReturn();
        String content = res.getResponse().getContentAsString();
        JsonNode node = content.isBlank() ? json.nullNode() : json.readTree(content);
        return new Response(res.getResponse().getStatus(), node);
    }

    protected Response book(Shop shop, long barberId, long serviceId, String startAt) throws Exception {
        return call(post("/api/public/" + shop.slug() + "/appointments"), null, Map.of(
                "barberId", barberId, "serviceId", serviceId, "startAt", startAt,
                "customerName", "Cliente Prueba", "customerPhone", "3001234567"));
    }

    protected static MockHttpServletRequestBuilder GET(String url) { return get(url); }
    protected static MockHttpServletRequestBuilder POST(String url) { return post(url); }
    protected static MockHttpServletRequestBuilder PUT(String url) { return put(url); }
    protected static MockHttpServletRequestBuilder PATCH(String url) { return patch(url); }
}
