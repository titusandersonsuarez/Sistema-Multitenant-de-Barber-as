-- BarberHub: esquema inicial.
-- Multitenancy por discriminador: todas las tablas de negocio llevan tenant_id
-- y Hibernate (@TenantId) filtra automáticamente cada consulta por la barbería actual.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE tenants (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(60)  NOT NULL UNIQUE,
    phone       VARCHAR(30),
    address     VARCHAR(200),
    timezone    VARCHAR(60)  NOT NULL DEFAULT 'America/Bogota',
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE barbers (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL REFERENCES tenants(id),
    name        VARCHAR(100) NOT NULL,
    bio         VARCHAR(300),
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX ix_barbers_tenant ON barbers(tenant_id);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL REFERENCES tenants(id),
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    barber_id     BIGINT REFERENCES barbers(id),
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email)
);

CREATE TABLE service_offerings (
    id                BIGSERIAL PRIMARY KEY,
    tenant_id         BIGINT       NOT NULL REFERENCES tenants(id),
    name              VARCHAR(100) NOT NULL,
    duration_minutes  INT          NOT NULL CHECK (duration_minutes BETWEEN 5 AND 480),
    price_cop         INT          NOT NULL CHECK (price_cop >= 0),
    active            BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX ix_services_tenant ON service_offerings(tenant_id);

CREATE TABLE working_hours (
    id           BIGSERIAL PRIMARY KEY,
    tenant_id    BIGINT   NOT NULL REFERENCES tenants(id),
    barber_id    BIGINT   NOT NULL REFERENCES barbers(id) ON DELETE CASCADE,
    day_of_week  SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time   TIME     NOT NULL,
    end_time     TIME     NOT NULL,
    CHECK (end_time > start_time)
);
CREATE INDEX ix_hours_barber ON working_hours(tenant_id, barber_id);

CREATE TABLE appointments (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       BIGINT       NOT NULL REFERENCES tenants(id),
    barber_id       BIGINT       NOT NULL REFERENCES barbers(id),
    service_id      BIGINT       NOT NULL REFERENCES service_offerings(id),
    code            VARCHAR(12)  NOT NULL UNIQUE,
    customer_name   VARCHAR(100) NOT NULL,
    customer_phone  VARCHAR(30)  NOT NULL,
    start_at        TIMESTAMP    NOT NULL,
    end_at          TIMESTAMP    NOT NULL,
    price_cop       INT          NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (end_at > start_at),
    -- Red de seguridad a nivel de BD: un barbero no puede tener dos citas activas que se crucen.
    CONSTRAINT no_overlap_per_barber EXCLUDE USING gist (
        barber_id WITH =,
        tsrange(start_at, end_at) WITH &&
    ) WHERE (status <> 'CANCELLED')
);
CREATE INDEX ix_appointments_day ON appointments(tenant_id, start_at);
