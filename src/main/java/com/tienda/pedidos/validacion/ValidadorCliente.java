package com.tienda.pedidos.validacion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;
import java.util.List;

// Eslabon 2: existencia y mora del cliente (solo se ejecuta si el eslabon de stock paso)
@Component
public class ValidadorCliente extends ValidadorPedido {
    private static final LocalTime HORA_CORTE = LocalTime.of(20, 0);

    private final JdbcTemplate jdbcTemplate;
    private final Clock reloj;

    public ValidadorCliente(JdbcTemplate jdbcTemplate, Clock reloj) {
        this.jdbcTemplate = jdbcTemplate;
        this.reloj = reloj;
    }

    @Override
    protected void ejecutar(ContextoPedido contexto) {
        Long clienteId = contexto.getRequest().getClienteId();
        List<String> tipos = jdbcTemplate.queryForList(
            "SELECT tipo_cliente FROM clientes WHERE id = ?", String.class, clienteId);
        if (tipos.isEmpty()) {
            contexto.rechazar("Cliente no registrado");
            return;
        }
        String tipo = tipos.get(0);
        contexto.setTipoCliente(tipo);

        if ("MOROSO".equals(tipo)) {
            Double deuda = jdbcTemplate.queryForObject(
                "SELECT SUM(monto) FROM facturas WHERE cliente_id = ? AND pagada = false",
                Double.class, clienteId);
            boolean antesDelCorte = LocalTime.now(reloj).isBefore(HORA_CORTE);
            if (deuda != null && deuda > 0 && antesDelCorte) {
                contexto.rechazar("Cliente con deuda pendiente: $" + deuda);
            }
        }
    }
}
