INSERT INTO productos (id, nombre, precio) VALUES (1, 'Teclado Mecanico', 250000.0);
INSERT INTO productos (id, nombre, precio) VALUES (2, 'Mouse Gamer', 150000.0);
INSERT INTO productos (id, nombre, precio) VALUES (3, 'Monitor 24', 600000.0);

INSERT INTO inventario (producto_id, stock) VALUES (1, 100);
INSERT INTO inventario (producto_id, stock) VALUES (2, 100);
INSERT INTO inventario (producto_id, stock) VALUES (3, 2);

-- Clientes para pruebas
INSERT INTO clientes (id, nombre, email, tipo_cliente) VALUES (101, 'Carlos VIP', 'carlos@vip.com', 'VIP');
INSERT INTO clientes (id, nombre, email, tipo_cliente) VALUES (102, 'Laura Frecuente', 'laura@frecuente.com', 'FRECUENTE');
INSERT INTO clientes (id, nombre, email, tipo_cliente) VALUES (103, 'Mario Estandar', 'mario@estandar.com', 'ESTANDAR');
INSERT INTO clientes (id, nombre, email, tipo_cliente) VALUES (104, 'Pedro Moroso', 'pedro@moroso.com', 'MOROSO');
INSERT INTO clientes (id, nombre, email, tipo_cliente, nit) VALUES (105, 'Industrias Andinas SAS', 'compras@andinas.com', 'ESTANDAR', '900123456-1');

-- Factura pendiente para el cliente moroso
INSERT INTO facturas (cliente_id, monto, pagada) VALUES (104, 350000.0, false);

-- Historial para cliente frecuente (4 pedidos: supera el umbral de 3 y obtiene 4% de descuento)
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (102, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (102, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (102, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
INSERT INTO pedidos (cliente_id, subtotal, descuento, impuesto, total, fecha, estado) VALUES (102, 100000, 0, 19000, 119000, CURRENT_TIMESTAMP, 'CONFIRMADO');
