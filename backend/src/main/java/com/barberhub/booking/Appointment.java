package com.barberhub.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Una cita. Las horas se guardan en hora local de la barbería (sin zona), porque así
 * las piensa el negocio: "el martes a las 3:00 pm". La zona vive en {@code Tenant.timezone}.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "barber_id", nullable = false)
    private Long barberId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    /** Código corto que recibe el cliente para consultar o cancelar su cita. */
    @Column(nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    /** Precio congelado al reservar, aunque el servicio cambie de precio después. */
    @Column(name = "price_cop", nullable = false)
    private int priceCop;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Appointment() {
    }

    public Appointment(Long barberId, Long serviceId, String code, String customerName, String customerPhone,
                       LocalDateTime startAt, LocalDateTime endAt, int priceCop) {
        this.barberId = barberId;
        this.serviceId = serviceId;
        this.code = code;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.startAt = startAt;
        this.endAt = endAt;
        this.priceCop = priceCop;
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public Long getBarberId() { return barberId; }
    public Long getServiceId() { return serviceId; }
    public String getCode() { return code; }
    public String getCustomerName() { return customerName; }
    public String getCustomerPhone() { return customerPhone; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public int getPriceCop() { return priceCop; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
