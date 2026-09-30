package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Black Friday activa: el mayor descuento entre tipo de cliente y campana gana
@SpringBootTest(properties = "promo.black-friday.activa=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CampanaBlackFridayTest {

    private static final double DELTA = 0.01;

    @Autowired
    private GestorPedidos gestor;

    @Test
    void clienteEstandarRecibeVeinticincoPorCiento() {
        // 1 teclado = 250.000 -> 25% -> 187.500 + 19% IVA
        ResultadoPedido r = gestor.procesarPedido(
            new PedidoRequest(103L, "mario@estandar.com", List.of(new ItemPedido(1L, 1))));
        assertTrue(r.isConfirmado());
        assertEquals(223_125.0, r.getTotal(), DELTA);
    }

    @Test
    void blackFridaySuperaElDescuentoVip() {
        // 4 teclados = 1.000.000 -> VIP daria 10%, Black Friday da 25% -> gana 25% -> 750.000 + 19% IVA
        ResultadoPedido r = gestor.procesarPedido(
            new PedidoRequest(101L, "carlos@vip.com", List.of(new ItemPedido(1L, 4))));
        assertTrue(r.isConfirmado());
        assertEquals(892_500.0, r.getTotal(), DELTA);
    }
}
