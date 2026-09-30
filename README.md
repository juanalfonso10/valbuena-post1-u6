# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción
Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño de Software. Un único proyecto Spring Boot (`pedidos-service`, en la raíz) con dos partes:

1. **Parte 1:** diagnóstico y refactorización de `GestorPedidos`, una clase que combinaba God Object y Spaghetti Code.
2. **Parte 2:** diagnóstico y corrección de un segundo antipatrón (Golden Hammer), introducido al hacer crecer el mismo proyecto con tres campañas de descuento.

El historial de commits refleja el proceso completo: código original → diagnóstico → refactorización, en ambas partes.

## Cómo ejecutar

```bash
mvn test
mvn spring-boot:run
```

## Decisiones de diseño

### Parte 1 — GestorPedidos


**Antipatrón identificado: God Object + Spaghetti Code combinados.**

La evidencia está en la versión original de `service/GestorPedidos.java` (primer commit del repositorio). El método `procesarPedido` ocupa las líneas 33–150 (118 líneas) y mezcla **seis responsabilidades** en una sola secuencia:

| Líneas | Responsabilidad | Nivel de abstracción |
|---|---|---|
| 36–49 | Validación de items y stock | SQL embebido + regla de negocio |
| 51–71 | Validación de cliente y mora con excepción por horario | SQL + regla + hora del sistema |
| 73–79 | Cálculo de subtotal (una consulta SQL por item) | SQL + aritmética |
| 81–102 | Cálculo de descuento por tipo de cliente e impuesto | Regla de negocio + SQL (pedidos previos) |
| 104–130 | Persistencia del pedido, detalle y descuento de inventario | SQL de escritura, sin transacción |
| 132–146 | Construcción y envío del correo | Formato de texto + integración externa |

- **God Object:** la clase tiene al menos seis razones distintas para cambiar. Un cambio en el formato del correo, en el esquema de la tabla `pedidos` o en las reglas de descuento obliga a editar el mismo método.
- **Spaghetti Code:** el flujo salta entre niveles de abstracción línea a línea (SQL → regla → formato de texto). La validación de mora llega a **3 niveles de anidamiento** (líneas 58 → 62 → 64: tipo MOROSO → deuda > 0 → antes de las 20:00), y el descuento a 2 niveles (líneas 83 → 84 y 91 → 94).
- **Prueba de extensibilidad:** agregar un nuevo tipo de cliente con reglas propias obliga a insertar otra rama `else if` dentro de las líneas 83–98 y a releer el método completo para no romper la persistencia ni el correo, que usan la misma variable `descuento`.

**Plan de refactorización:** separar las validaciones (orden real y corte anticipado) de las reglas de descuento (una regla por tipo de cliente, sin orden), y extraer persistencia y notificación a clases propias para que `GestorPedidos` solo orqueste.
