package com.barberhub.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.TenantId;

/** Un servicio que ofrece la barbería (corte, barba, cejas...). */
@Entity
@Table(name = "service_offerings")
public class ServiceOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "price_cop", nullable = false)
    private int priceCop;

    @Column(nullable = false)
    private boolean active = true;

    protected ServiceOffering() {
    }

    public ServiceOffering(String name, int durationMinutes, int priceCop) {
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.priceCop = priceCop;
    }

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public int getPriceCop() { return priceCop; }
    public void setPriceCop(int priceCop) { this.priceCop = priceCop; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
