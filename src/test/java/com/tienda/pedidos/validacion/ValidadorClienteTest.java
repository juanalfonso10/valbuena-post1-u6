package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Verifica las dos ramas del horario de corte con un reloj fijo, sin depender de la hora real
@SpringBootTest
class ValidadorClienteTest {

    private static final ZoneId ZONA = ZoneId.systemDefault();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ContextoPedido validarMorosoALas(LocalTime hora) {
        Clock reloj = Clock.fixed(LocalDate.now().atTime(hora).atZone(ZONA).toInstant(), ZONA);
        ValidadorCliente validador = new ValidadorCliente(jdbcTemplate, reloj);
        ContextoPedido contexto = new ContextoPedido(
            new PedidoRequest(104L, "pedro@moroso.com", List.of(new ItemPedido(1L, 1))));
        validador.validar(contexto);
        return contexto;
    }

    @Test
    void morosoDentroDelHorarioDeCorteSeRechaza() {
        ContextoPedido contexto = validarMorosoALas(LocalTime.of(10, 0));
        assertTrue(contexto.isRechazado());
        assertEquals("Cliente con deuda pendiente: $350000.0", contexto.getMotivoRechazo());
    }

    @Test
    void morosoFueraDelHorarioDeCorteSePermite() {
        ContextoPedido contexto = validarMorosoALas(LocalTime.of(21, 0));
        assertFalse(contexto.isRechazado());
        assertEquals("MOROSO", contexto.getTipoCliente());
    }
}
