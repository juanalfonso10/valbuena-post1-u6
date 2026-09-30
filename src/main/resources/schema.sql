DROP TABLE IF EXISTS detalle_pedido;
DROP TABLE IF EXISTS pedidos;
DROP TABLE IF EXISTS facturas;
DROP TABLE IF EXISTS clientes;
DROP TABLE IF EXISTS inventario;
DROP TABLE IF EXISTS productos;

CREATE TABLE productos (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(100),
    precio DOUBLE NOT NULL
);

CREATE TABLE inventario (
    producto_id BIGINT PRIMARY KEY,
    stock INT NOT NULL,
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE TABLE clientes (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(100),
    email VARCHAR(100),
    tipo_cliente VARCHAR(50),
    nit VARCHAR(20)
);

CREATE TABLE facturas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    monto DOUBLE NOT NULL,
    pagada BOOLEAN NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE TABLE pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    subtotal DOUBLE NOT NULL,
    descuento DOUBLE NOT NULL,
    impuesto DOUBLE NOT NULL,
    total DOUBLE NOT NULL,
    fecha TIMESTAMP NOT NULL,
    estado VARCHAR(50) NOT NULL
);

CREATE TABLE detalle_pedido (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);
