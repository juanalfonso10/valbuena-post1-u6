# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción
Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño de Software. Un único proyecto Spring Boot (`pedidos-service`, en la raíz) con dos partes:

1. **Parte 1:** diagnóstico y refactorización de `GestorPedidos`, una clase que combinaba God Object y Spaghetti Code.
2. **Parte 2:** diagnóstico y corrección de un segundo antipatrón (Golden Hammer), introducido al hacer crecer el mismo proyecto con tres campañas de descuento.

El historial de commits refleja el proceso completo: código original → diagnóstico → refactorización, en ambas partes.

## Estructura final

```
src/main/java/com/tienda/pedidos/
├── PedidosServiceApplication.java
├── config/RelojConfig.java                 ← Clock inyectable (horario de corte testeable)
├── dto/                                    ← PedidoRequest, ItemPedido, ResultadoPedido
├── validacion/                             ← Chain of Responsibility
│   ├── ContextoPedido.java
│   ├── ValidadorPedido.java                (eslabón abstracto)
│   ├── ValidadorStock.java                 (eslabón 1)
│   └── ValidadorCliente.java               (eslabón 2)
├── descuento/                              ← Strategy
│   ├── EstrategiaDescuento.java
│   ├── DescuentoVip.java, DescuentoFrecuente.java, DescuentoEstandar.java
│   ├── SelectorEstrategiaDescuento.java    (por tipo de cliente)
│   ├── DescuentoBlackFriday.java, DescuentoCorporativo.java, DescuentoVolumen.java  (Parte 2)
│   └── CalculadorDescuentoFinal.java       (Parte 2: combina cliente + campañas)
└── service/
    ├── GestorPedidos.java                  ← orquestador delgado
    ├── PedidoRepository.java               ← persistencia JDBC
    ├── NotificacionPedidoService.java      ← construcción y envío del correo
    ├── EmailService.java                   (interfaz)
    └── ConsolaEmailService.java            (implementación que imprime en consola)
```

## Cómo ejecutar

```bash
mvn test
mvn spring-boot:run
```

H2 se inicializa con `schema.sql` y `data.sql` (productos, inventario, clientes VIP/FRECUENTE/ESTANDAR/MOROSO/corporativo y una factura pendiente).

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

**Patrón aplicado:** Strategy. Las campañas tienen exactamente la misma forma que `DescuentoVip` o `DescuentoFrecuente`: calculan un porcentaje a partir de datos del pedido o del cliente. Se modelaron como `DescuentoBlackFriday`, `DescuentoCorporativo` y `DescuentoVolumen` (implementan `EstrategiaDescuento`), y `CalculadorDescuentoFinal` combina el descuento por tipo de cliente con el mayor de las campañas activas. La regla "gana el mayor" ahora vive en un único método legible.

**Alternativa descartada:** mantener las campañas en la cadena. Es precisamente la causa del antipatrón diagnosticado.

**Código descartado eliminado, no comentado:** `PromocionBlackFriday`, `PromocionCorporativo`, `PromocionVolumen` y el campo `descuentoCampana` se borraron por completo. Dejarlos comentados "por si acaso" es el origen de un Lava Flow. Su referencia histórica queda en el historial de Git, no en el código activo.

## Comparación antes / después

Las mismas pruebas se ejecutan sin cambios sobre cada versión del código. Que sigan pasando demuestra que la salida es equivalente.

| Caso de prueba | Resultado esperado | Original | Refactor Parte 1 | Golden Hammer | Corrección Parte 2 |
|---|---|---|---|---|---|
| Stock insuficiente (monitor x10) | Rechazado: "Stock insuficiente: producto 3" | ✔ | ✔ | ✔ | ✔ |
| Cliente inexistente (999) | Rechazado: "Cliente no registrado" | ✔ | ✔ | ✔ | ✔ |
| Moroso antes de las 20:00 | Rechazado: "Cliente con deuda pendiente: $350000.0" | ✔ | ✔ | ✔ | ✔ |
| Moroso después de las 20:00 | Confirmado | ✔ | ✔ | ✔ | ✔ |
| VIP, 4 teclados ($1.000.000) | 10% → total $1.071.000 | ✔ | ✔ | ✔ | ✔ |
| Frecuente, 2 mouse ($300.000) | 4% → total $342.720 | ✔ | ✔ | ✔ | ✔ |
| Corporativo con NIT, 1 teclado | 10% → total $267.750 | — | — | ✔ | ✔ |
| Volumen, 21 mouse ($3.150.000) | 12% → total $3.298.680 | — | — | ✔ | ✔ |
| Black Friday, estándar, 1 teclado | 25% → total $223.125 | — | — | ✔ | ✔ |
| Black Friday + VIP, 4 teclados | gana 25% → total $892.500 | — | — | ✔ | ✔ |

Pruebas: `GestorPedidosTest` (5 pedidos base), `ValidadorClienteTest` (horario de corte con reloj fijo), `CampanasDescuentoTest` y `CampanaBlackFridayTest` (campañas).

## Herramientas utilizadas
- Java 17, Spring Boot 3.2, Spring JDBC, H2, JUnit 5
- Apache Maven, Git, GitHub

## Conclusiones
Diagnosticar un antipatrón exige evidencia del código (líneas, responsabilidades, niveles de anidamiento), no solo nombrarlo: fue ese conteo el que mostró que `procesarPedido` tenía seis razones para cambiar. La Parte 1 dejó una lección que la Parte 2 puso a prueba: un patrón se justifica por la forma del problema (Chain of Responsibility por orden y corte anticipado, Strategy por reglas intercambiables sin orden), no porque ya exista en el proyecto. Reutilizar la cadena para las campañas funcionaba, pero rompía el contrato de `ValidadorPedido` y escondía la regla de combinación en un campo mutable. Mantener las mismas pruebas en cada versión fue lo que permitió refactorizar dos veces con la seguridad de no cambiar el comportamiento observable.
