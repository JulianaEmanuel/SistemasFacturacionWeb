package com.facturacion.sistemaweb.config;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
@Profile("pruebas")
class AmbientePruebasConfiguration {
    @Bean
    SmartInitializingSingleton verificarBasePruebas(JdbcTemplate jdbc) {
        return () -> {
            String base = jdbc.queryForObject("select current_database()", String.class);
            if (!"sistemafacturacionweb_pruebas".equals(base)) {
                throw new IllegalStateException("El perfil pruebas requiere la base sistemafacturacionweb_pruebas.");
            }
        };
    }
}
