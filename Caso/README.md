# TASF-B2B — Componente Planificador 

## Contexto del problema

Tasf.B2B es una empresa de transporte aereo que traslada maletas entre aeropuertos de America, Asia y Europa. El sistema debe planificar rutas de maletas desde un aeropuerto origen hasta un aeropuerto destino, usando vuelos disponibles, respetando plazos de entrega y capacidades.

Este repositorio contiene unicamente el **componente planificador** implementado en Java 21, usando ALNS (Adaptive Large Neighborhood Search) como algoritmo principal de planificacion.

---

## Archivos de datos — carpeta `data/`

### `aeropuertos.txt`
- Codificacion: UTF-16 BE con BOM
- Formato por linea: `id   CODIGO   Ciudad   Pais   alias   GMT   capacidad   Latitud   Longitud`
- Tiene 3 lineas de cabecera antes del primer aeropuerto (ignorar al parsear)
- 30 aeropuertos en total: 10 America del Sur, 10 Europa, 10 Asia
- El campo `codigo` (ICAO, 4 letras) es la clave principal
- El campo `GMT` indica el desfase horario (entero con signo: -5, +2, etc.)
- El campo `capacidad` es la capacidad del almacen del aeropuerto (500–800 maletas)
- Los campos `alias`, `Latitud` y `Longitud` se leen del archivo pero no se usan en el modelo

### `planes_vuelo.txt`
- Codificacion: UTF-8
- Formato por linea: `ORIGEN-DESTINO-HH:MM-HH:MM-CCCC`
- Campos: origen (ICAO), destino (ICAO), hora_salida, hora_llegada, capacidad del vuelo
- Las horas se interpretan como hora local de cada aeropuerto y se normalizan a GMT para comparacion
- Si `llegadaMinNormalizada < salidaMinNormalizada` tras normalizar, la llegada ocurre al dia siguiente (sumar 1440)
- La capacidad viene como string de 4 digitos (ej: `0300`), parsear a entero
- 2866 vuelos en total, cubre los 30 aeropuertos

### `envios/_envios_XXXX_.txt` (30 archivos, uno por aeropuerto origen)
- Codificacion: UTF-8
- Formato por linea: `id_envio-aaaammdd-hh-mm-DEST-cantidad-id_cliente`
- Ejemplo: `000000001-20260102-01-06-SPIM-002-0001291`
- El **origen del envio se deduce del nombre del archivo** (ej: `_envios_EKCH_.txt` → origen = `EKCH`)
- `id_envio`: 9 digitos numericos
- `fecha`: formato `aaaammdd`
- `hh`: hora, maximo 23
- `mm`: minuto, maximo 59
- `cantidad`: string de 3 digitos, parsear a entero
- `id_cliente`: 7 digitos — presente en el archivo pero no utilizado por el planificador
- Los 30 archivos se cargan en paralelo; hay un limite configurable de envios por archivo y un limite global

---

## Modelo de clases

### Clases de dominio (`tasf.modelo`)

#### `Aeropuerto`

| Campo | Tipo | Descripcion |
|---|---|---|
| `codigo` | `String` | Codigo ICAO de 4 letras. Clave principal. |
| `ciudad` | `String` | Ciudad del aeropuerto. |
| `pais` | `String` | Pais del aeropuerto. |
| `continente` | `String` | Codigo normalizado del continente. Validado contra `Continentes`. |
| `gmt` | `int` | Desfase horario del aeropuerto respecto a GMT. |
| `capacidadMaxima` | `int` | Capacidad del almacen del aeropuerto. |

#### `Vuelo`

| Campo | Tipo | Descripcion |
|---|---|---|
| `origenCodigo` | `String` | Codigo ICAO del aeropuerto de salida. |
| `destinoCodigo` | `String` | Codigo ICAO del aeropuerto de llegada. |
| `horaSalidaLocalTexto` | `String` | Hora original del archivo, por ejemplo `23:08`. |
| `horaLlegadaLocalTexto` | `String` | Hora original del archivo, por ejemplo `17:13`. |
| `salidaMinLocal` | `int` | Minutos desde medianoche en hora local del origen. |
| `llegadaMinLocal` | `int` | Minutos desde medianoche en hora local del destino. |
| `salidaMinNormalizada` | `int` | Salida convertida a referencia GMT comun. |
| `llegadaMinNormalizada` | `int` | Llegada convertida a referencia GMT comun. Puede superar 1440 si llega al dia siguiente. |
| `capacidadMaxima` | `int` | Capacidad total del vuelo en maletas. |

#### `Envio`

| Campo | Tipo | Descripcion |
|---|---|---|
| `id` | `String` | Identificador del envio. |
| `origenCodigo` | `String` | Codigo ICAO deducido del nombre del archivo. |
| `destinoCodigo` | `String` | Codigo ICAO destino del envio. |
| `timestampMinutos` | `int` | Minutos transcurridos desde `inicioSimulacion`, normalizados a GMT del origen. |
| `cantidad` | `int` | Cantidad de maletas del envio. |

#### `Ruta`

| Campo | Tipo | Descripcion |
|---|---|---|
| `vuelos` | `List<Vuelo>` | Lista ordenada de vuelos que resuelve el traslado. |

#### `AsignacionEnvio`

| Campo | Tipo | Descripcion |
|---|---|---|
| `envio` | `Envio` | Envio planificado. |
| `ruta` | `Ruta` | Ruta asignada al envio. |

#### `Solucion`

| Campo | Tipo | Descripcion |
|---|---|---|
| `asignaciones` | `List<AsignacionEnvio>` | Conjunto de envios asignados a rutas. |
| `valorObjetivo` | `double` | Valor numerico de evaluacion de la solucion. |

---

## Restricciones del negocio

1. **Plazo de entrega mismo continente**: maximo 1 dia (1440 minutos). La entrega se mide como `llegadaFinal + tiempoLiberacionDestinoMin - timestampEnvio` y debe ser `<= plazoMaximo`.
2. **Plazo de entrega distinto continente**: maximo 2 dias (2880 minutos). Misma regla de liberacion aplica.
3. **Capacidad de vuelo**: la suma de maletas asignadas a un vuelo no puede superar su capacidad maxima
4. **Conexion entre vuelos**: `vuelo2.salidaMinNormalizada >= vuelo1.llegadaMinNormalizada + tiempoEscala`
5. **Compatibilidad temporal**: un envio no puede asignarse a un vuelo que salga antes de su `timestampMinutos`
6. **Capacidad de almacen**: cada aeropuerto tiene limite de maletas almacenadas simultaneamente. En el aeropuerto destino, el envio ocupa almacen durante `tiempoLiberacionDestinoMin` minutos tras la llegada antes de liberar el espacio.

---

## Logica de tiempo

- Los horarios de `planes_vuelo.txt` se interpretan como **hora local del aeropuerto correspondiente**:
  - `hora_salida`: hora local del aeropuerto origen
  - `hora_llegada`: hora local del aeropuerto destino
- Para comparar tiempos entre vuelos y validar conexiones, ambas horas se **normalizan a una referencia comun usando el GMT** de cada aeropuerto.
- Si tras normalizar `llegadaMinNormalizada < salidaMinNormalizada`, la llegada ocurre al dia siguiente y se suma `1440`.
- `inicioSimulacion` define desde que momento empieza a correr la simulacion. Es configurable.
- El `timestampMinutos` de un envio representa los minutos transcurridos desde `inicioSimulacion`, convirtiendo la fecha/hora local del envio usando el GMT del aeropuerto origen.
- La compatibilidad temporal entre vuelos conectados se valida con:
  - `vueloSiguiente.salidaMinNormalizada >= vueloAnterior.llegadaMinNormalizada + tiempoEscala`
- Para determinar si un envio tiene plazo de 24h o 48h, se compara el atributo `continente` del aeropuerto origen con el del destino.
- El `tiempoLiberacionDestinoMin` afecta dos reglas:
  - **Entrega**: `tiempoEntrega = (llegadaFinal + tiempoLiberacionDestinoMin) - timestampEnvio`, debe cumplir `<= plazoMaximo`.
  - **Almacen destino**: el envio ocupa capacidad en `[llegadaFinal, llegadaFinal + tiempoLiberacionDestinoMin)` antes de liberar el espacio.
- El `tiempoEnRed` reportado (BFS y evaluador) incluye la liberacion: `tiempoEnRed = llegadaFinal + tiempoLiberacionDestinoMin - timestampEnvio`.

### Respaldo estadistico de la decision de usar GMT

Se evaluaron los **2866 vuelos** del archivo comparando dos interpretaciones:

1. **Sin GMT**: convertir `HH:MM` a minutos y, si `llegada < salida`, sumar `1440`.
2. **Con GMT**: interpretar salida/llegada en hora local y normalizar usando el GMT de origen y destino.

Resumen observado:

- **Sin GMT**
  - duracion minima: `0 min`
  - duracion maxima: `1430 min` (`23h 50m`)
  - promedio: `447.33 min` (`7h 27m`)
  - vuelos con duracion mayor a `20h`: `248`

- **Con GMT**
  - duracion minima: `13 min`
  - duracion maxima: `1269 min` (`21h 09m`)
  - promedio: `533.24 min` (`8h 53m`)
  - vuelos con duracion mayor a `20h`: `32`

Ejemplos representativos:

- `SKBO-EKCH-01:49-20:37`
  - sin GMT: `18h 48m`
  - con GMT: `11h 48m`

- `EKCH-SKBO-01:37-06:25`
  - sin GMT: `4h 48m`
  - con GMT: `11h 48m`

Conclusion: usar GMT corrige asimetrias poco realistas entre ida y vuelta y reduce drasticamente los vuelos con duraciones excesivas. La decision de modelado es normalizar los horarios de vuelos usando GMT.

---

## Flujo de ejecucion

```
Main
 └─ CargadorDatos          — carga aeropuertos, vuelos y envios (30 archivos en paralelo)
 └─ GeneradorSolucionInicial — construye solucion inicial factible (heuristica 2 fases)
 └─ PreparadorALNS          — adapta la solucion dominio a representacion compacta ALNS
 └─ ALNSEngine              — optimiza mediante ciclo destroy-repair con SA y pesos adaptativos
 └─ EvaluadorSolucion       — valida la solucion final contra todas las restricciones
 └─ GeneradorReporteEjecucion — genera reporte JSON de la corrida (opcional)
```

### Generador de solucion inicial

Heuristica de dos fases con exploracion BFS guiada:

- **Fase 1**: genera rutas candidatas para cada envio usando BFS con dominancia de labels, beam search por aeropuerto/nivel, y frontera global. Prioriza envios criticos segun el estado de la red (escasez de vuelos, tension por ocupacion).
- **Fase 2**: asigna cada envio a la mejor ruta candidata viable, confirmando la asignacion y propagando el impacto en capacidades y dependencias. Si no hay candidatas viables, regenera.

Clases principales:

| Clase | Descripcion |
|---|---|
| `GeneradorSolucionInicial` | Orquesta las dos fases y mantiene el estado global de construccion |
| `ExploradorRutasCandidatas` | BFS con dominancia, beam search, y poda por capacidad/margen/ciclo |
| `MotorPrioridadRed` | Calcula la prioridad de cada pedido segun el estado de tension de la red |
| `GestorCandidatasPedido` | Administra el pool de rutas candidatas por pedido |
| `GestorDependenciasVuelos` | Rastrea que pedidos dependen de cada vuelo para invalidar caches |
| `CoordinadorAsignacionPedido` | Ejecuta el intento de asignacion y confirmacion por pedido |
| `AplicadorAsignacion` | Aplica una asignacion a la solucion dominio actualizando capacidades |

### ALNS

Ciclo de optimizacion sobre una representacion compacta (`SolucionALNS`):

- **Operadores de destruccion**: `RandomRemoval`, `CriticalFlightRemoval`, `StorageConflictRemoval`
- **Operadores de reparacion**: `GreedyRepair`, `RegretRepair`
- **Criterio de aceptacion**: Simulated Annealing
- **Seleccion adaptativa**: `AdaptiveOperatorSelector` actualiza pesos segun la calidad de cada operador

---

## Parametros configurables

### `Main.java` — constantes de corrida

| Constante | Valor actual | Descripcion |
|---|---|---|
| `N_ENVIOS_POR_ARCHIVO_PRUEBA` | `2_000` | Maximo de envios leidos por archivo |
| `LIMITE_ENVIOS_PRUEBA` | `60_000` | Limite global de envios cargados |
| `ALNS_MAX_ITER_PRUEBA` | `50` | Maximo de iteraciones ALNS |
| `ALNS_MAX_TIEMPO_MS` | `180_000` | Tiempo maximo de ALNS en ms (3 min) |
| `ALNS_MAX_CANDIDATOS_POR_ENVIO_REGRET` | `96` | Candidatos evaluados por RegretRepair |
| `ALNS_LOG_DIAGNOSTICO` | `false` | Activa logs detallados por iteracion ALNS |
| `REPORTE_EJECUCION_ACTIVO` | `false` | Genera reporte JSON al terminar |

### `ConfiguracionSimulacion`

| Campo | Valor actual | Descripcion |
|---|---|---|
| `inicioSimulacion` | configurable | Instante de inicio de la simulacion (hora local) |
| `gmtInicioSimulacion` | configurable | Offset GMT del contexto en que se define `inicioSimulacion` |
| `nEnviosPorArchivo` | `200` | Cantidad de envios a leer por archivo |
| `tiempoEscala` | `10` | Minutos minimos de espera entre llegada y siguiente salida en escala. Regla: `salida_i+1 >= llegada_i + tiempoEscala`. |
| `tiempoLiberacionDestinoMin` | `10` | Minutos que el envio ocupa almacen en el aeropuerto destino tras llegar antes de liberarlo. Afecta deadline de entrega y ocupacion de almacen destino. |
| `maxIteracionesAlns` | `100` | Iteraciones del ciclo ALNS |

Ambos parametros (`tiempoEscala` y `tiempoLiberacionDestinoMin`) se propagan en cadena:
`ConfiguracionSimulacion` → `GeneradorSolucionInicial` / `ExploradorRutasCandidatas` → `PreparadorALNS` → `ProblemaALNS` → `SolucionALNS` / `EvaluadorInsercion` → `EvaluadorSolucion` (validacion final). Cambiarlos a otro valor no requiere modificar codigo del algoritmo.

### `ConfiguracionGenerador` — explorador y heuristica

| Campo | Default | Descripcion |
|---|---|---|
| `maxRutasCandidatasPorPedido` | `10` | Rutas candidatas a generar por pedido |
| `maxExpansionesPorNodo` | `5` | Expansiones maximas por nodo BFS |
| `maxReintentosCandidatasPorPedido` | `2` | Regeneraciones permitidas si no hay viables |
| `maxPasadasPermitido` | `3` | Pasadas maximas sobre todos los pedidos |
| `beamWidthPorAeropuertoNivel` | `12` | Beam width por aeropuerto y nivel BFS |
| `maxEstadosFronteraGlobal` | `4000` | Frontera global maxima del BFS |
| `maxLabelsPorAeropuertoNivel` | `8` | Labels por aeropuerto en dominancia post-expansion |
| `maxLabelsPreexpandirPorGrupo` | `4` | Labels por grupo en dominancia pre-expansion |
| `maxProfundidadTecnicaExploracion` | `30` | Profundidad maxima del BFS |
| `maxNodosTecnicosExploracionPorPedido` | `50_000` | Nodos maximos evaluados por pedido |
| `maxVuelosInspeccionadosPorEstado` | `24` | Vuelos candidatos a inspeccionar por estado BFS |

---

## Estructura del proyecto

```
src/tasf/
├── Main.java
├── modelo/
│   ├── Aeropuerto.java
│   ├── Vuelo.java
│   ├── Envio.java
│   ├── Ruta.java
│   ├── AsignacionEnvio.java
│   ├── Solucion.java
│   └── Continentes.java
├── datos/
│   ├── CargadorDatos.java
│   ├── ParseadorEnvio.java
│   ├── LectorArchivoEnvios.java
│   └── LectorStreamingEnvios.java
├── tiempo/
│   ├── ConfiguracionSimulacion.java
│   └── ConversorTiempo.java
├── evaluacion/
│   ├── EvaluadorSolucion.java
│   ├── ResultadoEvaluacion.java
│   └── ReporteEvaluacion.java
├── construccion/
│   ├── GeneradorSolucionInicial.java
│   ├── ConfiguracionGenerador.java
│   ├── RutaParcial.java
│   ├── EstadoRed.java
│   ├── ResumenConstruccion.java
│   ├── asignacion/
│   │   ├── CoordinadorAsignacionPedido.java
│   │   ├── AplicadorAsignacion.java
│   │   └── IntentoPedidoResultado.java
│   ├── candidatas/
│   │   └── GestorCandidatasPedido.java
│   ├── cache/
│   │   └── GestorCachesConstruccion.java
│   ├── dependencia/
│   │   └── GestorDependenciasVuelos.java
│   ├── exploracion/
│   │   ├── ExploradorRutasCandidatas.java
│   │   ├── DominanciaExplorador.java
│   │   ├── CriticidadExplorador.java
│   │   ├── PrecomputacionesExplorador.java
│   │   ├── ProveedorCandidatosPorOrigen.java
│   │   ├── OperacionesOrdenRutas.java
│   │   └── MetricasExploracionPedido.java
│   ├── metricas/
│   │   ├── EstadisticasExpansion.java
│   │   └── MetricasHotspotsConstruccion.java
│   └── prioridad/
│       ├── MotorPrioridadRed.java
│       ├── CriticidadRuta.java
│       └── ContextoViabilidad.java
├── algoritmo/alns/
│   ├── ALNSEngine.java
│   ├── PreparadorALNS.java
│   ├── acceptance/
│   │   ├── AcceptanceCriterion.java
│   │   ├── EstadoAceptacion.java
│   │   └── SimulatedAnnealingAcceptance.java
│   ├── core/
│   │   ├── ConfiguracionALNS.java
│   │   ├── ProblemaALNS.java
│   │   ├── ResultadoALNS.java
│   │   ├── SolucionALNS.java  (en state/)
│   │   ├── RutaAsignadaCompacta.java
│   │   ├── ComparadorSolucionALNS.java
│   │   └── ALNSStrings.java
│   ├── metrics/
│   │   ├── MetricasALNS.java
│   │   └── ScoreLexicograficoALNS.java
│   ├── operators/
│   │   ├── destroy/
│   │   │   ├── DestroyOperator.java
│   │   │   ├── RandomRemovalOperator.java
│   │   │   ├── CriticalFlightRemovalOperator.java
│   │   │   └── StorageConflictRemovalOperator.java
│   │   └── repair/
│   │       ├── RepairOperator.java
│   │       ├── GreedyRepairOperator.java
│   │       ├── RegretRepairOperator.java
│   │       ├── EvaluadorInsercion.java
│   │       └── ResultadoInsercion.java
│   ├── selection/
│   │   └── AdaptiveOperatorSelector.java
│   ├── state/
│   │   ├── SolucionALNS.java
│   │   ├── BolsaEnteros.java
│   │   └── DeltaLog.java
│   └── storage/
│       └── EstadoAlmacenAeropuerto.java
└── reportes/
    ├── GeneradorReporteEjecucion.java
    ├── ReporteEjecucionData.java
    ├── AlnsSnapshot.java
    ├── ValidacionFinalSnapshot.java
    ├── MetadatosReporteSnapshot.java
    ├── archivos/
    │   └── GestorArchivosReporte.java
    ├── mapper/
    │   └── ReporteEjecucionMapper.java
    └── plantilla/
        ├── CargadorPlantillaReporte.java
        └── RenderizadorPlantillaReporte.java
```

---

## Compilacion y ejecucion

No usa Maven ni Gradle. Las dependencias (SLF4J + Logback) estan en `lib/`. El output compilado va a `out/production/tasf-planificador/`.

**Compilar:**
```bash
javac -cp "lib/*" -d out/production/tasf-planificador -sourcepath src $(find src -name "*.java")
```

**Ejecutar:**
```bash
java -cp "out/production/tasf-planificador:lib/*" tasf.Main
```
