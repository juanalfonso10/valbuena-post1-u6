package com.tienda.pedidos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// El reloj se inyecta para poder probar la regla de horario de corte con una hora fija
@Configuration
public class RelojConfig {
    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
