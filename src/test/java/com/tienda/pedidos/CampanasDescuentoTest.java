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

// Campanas Corporativo y Volumen (Black Friday desactivada por defecto)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CampanasDescuentoTest {

    private static final double DELTA = 0.01;

    @Autowired
    private GestorPedidos gestor;

    @Test
    void clienteConNitRecibeDescuentoCorporativo() {
        // 1 teclado = 250.000 -> cliente ESTANDAR con NIT -> 10% -> 225.000 + 19% IVA
        ResultadoPedido r = gestor.procesarPedido(
            new PedidoRequest(105L, "compras@andinas.com", List.of(new ItemPedido(1L, 1))));
        assertTrue(r.isConfirmado());
        assertEquals(267_750.0, r.getTotal(), DELTA);
    }

    @Test
    void pedidoDeMasDeVeinteUnidadesRecibeDescuentoPorVolumen() {
        // 21 mouse = 3.150.000 -> cliente ESTANDAR, mas de 20 unidades -> 12% -> 2.772.000 + 19% IVA
        ResultadoPedido r = gestor.procesarPedido(
            new PedidoRequest(103L, "mario@estandar.com", List.of(new ItemPedido(2L, 21))));
        assertTrue(r.isConfirmado());
        assertEquals(3_298_680.0, r.getTotal(), DELTA);
    }

    @Test
    void sinCampanaActivaElClienteEstandarNoRecibeDescuento() {
        // 1 teclado = 250.000 -> ESTANDAR sin NIT, pocas unidades, Black Friday inactiva -> 0% + 19% IVA
        ResultadoPedido r = gestor.procesarPedido(
            new PedidoRequest(103L, "mario@estandar.com", List.of(new ItemPedido(1L, 1))));
        assertTrue(r.isConfirmado());
        assertEquals(297_500.0, r.getTotal(), DELTA);
    }
}
