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

**Patrones aplicados:**
- **Chain of Responsibility** para las validaciones (`ValidadorStock` → `ValidadorCliente`): tienen una dependencia real de orden y necesitan corte anticipado. Si falla el stock, ni siquiera se consulta la mora.
- **Strategy** para el descuento por tipo de cliente (`DescuentoVip`, `DescuentoFrecuente`, `DescuentoEstandar` + `SelectorEstrategiaDescuento`): siempre aplica exactamente una regla, elegida por tipo, sin orden entre ellas.
- La persistencia pasa a `PedidoRepository` y la notificación a `NotificacionPedidoService`. `GestorPedidos` queda como orquestador de ~50 líneas.

**Alternativas descartadas:**
- *Validaciones como lista de `Predicate<ContextoPedido>` en un método `validarTodo()`*: evalúa todos los predicados aunque el primero ya haya fallado y no permite que un validador decida no delegar. Se pierde el corte anticipado que sí da la cadena.
- *Descuento como un eslabón más de la cadena*: las reglas de descuento no tienen orden ni necesitan cortar el flujo. Modelarlas como cadena obligaría a inventar un mecanismo para que solo un eslabón escriba el descuento. Un mapa de selección directa lo resuelve con menos indirección.

**Mejora adicional:** `ValidadorCliente` recibe un `java.time.Clock` inyectado (`config/RelojConfig`) en lugar de llamar a `LocalTime.now()`. Así, `ValidadorClienteTest` verifica **las dos ramas** del horario de corte (10:00 rechaza, 21:00 permite) sin depender de la hora en que se corran las pruebas.

### Parte 2 — Crecimiento del proyecto

**Antipatrón identificado: Golden Hammer.**

La evidencia está en el commit `feat: agregar 3 campanas de descuento como eslabones de la cadena de validacion`:

- `PromocionBlackFriday`, `PromocionCorporativo` y `PromocionVolumen` **extienden `ValidadorPedido`** (línea 9, 8 y 7 de cada archivo), cuyo contrato es "decidir si el pedido continúa o se rechaza". Sin embargo, **ninguna llama a `contexto.rechazar(...)`**: solo escriben un porcentaje con `aplicarDescuentoCampana` (líneas 19, 20 y 13).
- **No hay dependencia de orden:** ejecutar `PromocionVolumen` antes que `PromocionCorporativo` da el mismo resultado. En cambio, `ValidadorStock` sí debe ir antes que `ValidadorCliente`. La propiedad que justificaba la cadena (orden + corte anticipado) no existe para las campañas.
- **Estado mutable compartido:** se agregó `descuentoCampana` a `ContextoPedido` (línea 13) para que tres clases compitan por sobrescribirlo (`if (valor > this.descuentoCampana)`, línea 33). Si mañana dos campañas debieran **sumarse** en vez de tomar el máximo, la regla quedaría escondida dentro del contexto y no en un lugar de cálculo explícito.
- `GestorPedidos` pasó a encadenar 5 eslabones (línea 31) y a mezclar dos fuentes de descuento con `Math.max(descuentoTipoCliente, contexto.getDescuentoCampana())` (línea 49).
- **Por qué es Golden Hammer:** se eligió Chain of Responsibility porque "ya funcionó" en la Parte 1 y los eslabones "ya sabían conectarse", no porque el nuevo problema tuviera forma de cadena.

**Plan de corrección:** mover las tres campañas a `EstrategiaDescuento`, igual que los descuentos por tipo de cliente, y dejar en la cadena solo `ValidadorStock` y `ValidadorCliente`.
