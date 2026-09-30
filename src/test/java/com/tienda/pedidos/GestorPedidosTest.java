package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Cinco pedidos de referencia: las mismas pruebas deben pasar antes y despues de refactorizar.
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GestorPedidosTest {

    private static final double DELTA = 0.01;

    @Autowired
    private GestorPedidos gestor;

    @Test
    void stockInsuficienteRechazaElPedido() {
        // Monitor 24: stock 2, se piden 10
        ResultadoPedido r = gestor.procesarPedido(
            new PedidoRequest(101L, "carlos@vip.com", List.of(new ItemPedido(3L, 10))));
        assertFalse(r.isConfirmado());
        assertEquals("Stock insuficiente: producto 3", r.getMotivoRechazo());
    }

    @Test
    void clienteInexistenteRechazaElPedido() {
        ResultadoPedido r = gestor.procesarPedido(
            new PedidoRequest(999L, "nadie@tienda.com", List.of(new ItemPedido(1L, 1))));
        assertFalse(r.isConfirmado());
        assertEquals("Cliente no registrado", r.getMotivoRechazo());
    }

    @Test
    void clienteMorosoSegunHorarioDeCorte() {
        ResultadoPedido r = gestor.procesarPedido(
            new PedidoRequest(104L, "pedro@moroso.com", List.of(new ItemPedido(1L, 1))));
        if (LocalTime.now().isBefore(LocalTime.of(20, 0))) {
            assertFalse(r.isConfirmado());
            assertEquals("Cliente con deuda pendiente: $350000.0", r.getMotivoRechazo());
        } else {
            assertTrue(r.isConfirmado());
            assertEquals(297_500.0, r.getTotal(), DELTA);
        }
    }

    @Test
    void clienteVipRecibeDiezPorCiento() {
        // 4 teclados = 1.000.000 -> VIP > 500.000 -> 10% -> 900.000 + 19% IVA
        ResultadoPedido r = gestor.procesarPedido(
            new PedidoRequest(101L, "carlos@vip.com", List.of(new ItemPedido(1L, 4))));
        assertTrue(r.isConfirmado());
        assertEquals(1_071_000.0, r.getTotal(), DELTA);
    }

    @Test
    void clienteFrecuenteRecibeCuatroPorCiento() {
        // 2 mouse = 300.000 -> FRECUENTE con 4 pedidos previos -> 4% -> 288.000 + 19% IVA
        ResultadoPedido r = gestor.procesarPedido(
            new PedidoRequest(102L, "laura@frecuente.com", List.of(new ItemPedido(2L, 2))));
        assertTrue(r.isConfirmado());
        assertEquals(342_720.0, r.getTotal(), DELTA);
    }
}
