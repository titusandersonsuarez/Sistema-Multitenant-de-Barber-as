package com.barberhub.tenancy;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conecta {@link TenantContext} con Hibernate. Cada entidad con un campo {@code @TenantId}
 * queda filtrada automáticamente por la barbería actual en lecturas, y el tenant se asigna
 * solo al insertar. Así ningún repositorio tiene que acordarse de filtrar a mano.
 */
@Configuration
public class TenancyHibernateConfig {

    @Bean
    public CurrentTenantIdentifierResolver<Long> tenantIdentifierResolver() {
        return new CurrentTenantIdentifierResolver<>() {
            @Override
            public Long resolveCurrentTenantIdentifier() {
                return TenantContext.get();
            }

            @Override
            public boolean validateExistingCurrentSessions() {
                return true;
            }

            @Override
            public boolean isRoot(Long tenantId) {
                return tenantId != null && tenantId == TenantContext.ROOT;
            }
        };
    }

    @Bean
    public HibernatePropertiesCustomizer tenancyCustomizer(CurrentTenantIdentifierResolver<Long> resolver) {
        return props -> props.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, resolver);
    }
}
