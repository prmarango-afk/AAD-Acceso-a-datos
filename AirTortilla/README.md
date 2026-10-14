# AirTortilla Booking System

Aplicación de consola en Java que lee las reservas de AirTortilla desde un fichero CSV, asigna
**localizadores** a los pasajeros coincidentes y **divide las reservas en ficheros por país de destino**.

## Stack tecnológico

| Elemento | Versión / elección |
|---|---|
| Lenguaje | Java 21 (Temurin) |
| Build | Maven |
| Interfaz | Terminal (consola) |
| Persistencia | Ficheros CSV |
| IDE | IntelliJ IDEA |

## Cómo ejecutarlo

1. Abrir la carpeta `AirTortilla` en IntelliJ (*File → Open*). IntelliJ detecta el `pom.xml`.
2. Copiar el CSV de datos en `data/AirTortilla_Reservas_2020.csv` (no está en el repo; ver formato abajo).
   Para probar rápido se puede usar `data/ejemplo_reservas.csv` (opción 1 del menú → escribir su ruta).
3. Ejecutar `com.airtortilla.Main` (botón ▶ junto al `main`).

Opcionalmente se pueden pasar argumentos: `[ficheroEntrada] [carpetaSalida]`
(por defecto `data/AirTortilla_Reservas_2020.csv` y `output/`).

## Formato del CSV de entrada

- Codificación **UTF-8**, separador **`;`**, primera línea = cabecera.
- El fichero de datos **no se sube al repositorio** (está en `.gitignore`). Hay un ejemplo en `data/ejemplo_reservas.csv`.

| Columna | Tipo | Formato / restricciones | Ejemplo |
|---|---|---|---|
| `IdReserva` | String | Único, obligatorio | `R0001` |
| `NombrePasajero` | String | Obligatorio | `María Fernández` |
| `Origen` | String | Ciudad | `Madrid` |
| `PaisOrigen` | String | Código ISO 3166-1 alfa-2 | `ES` |
| `Destino` | String | Ciudad | `Paris` |
| `PaisDestino` | String | Código ISO 3166-1 alfa-2 (2 letras) | `FR` |
| `FechaReserva` | Fecha | `yyyy-MM-dd` | `2020-03-15` |
| `NumeroVuelo` | String | Obligatorio | `AT1234` |

```csv
IdReserva;NombrePasajero;Origen;PaisOrigen;Destino;PaisDestino;FechaReserva;NumeroVuelo
R0061;María Fernández;Madrid;ES;Paris;FR;2020-03-15;AT1234
```

Las líneas incorrectas (columnas de más o de menos, campos vacíos, fecha inválida, país que no es ISO
de 2 letras o id repetido) se descartan y se informa de ellas en el feedback de la carga.

## Ficheros de salida

Se generan en `output/` con el nombre **`AirTortilla_XX_YYYY_MM_DD.csv`**, donde `XX` es el código ISO del
país de destino y `YYYY_MM_DD` la fecha de la reserva. Tienen las mismas columnas que la entrada más
`Localizador` (vacío si la reserva no pertenece a ningún grupo).

## Localizadores

Cuando **2 o más reservas coinciden en nombre del pasajero, origen y destino**, todas reciben el mismo
localizador (`LOC-0001`, `LOC-0002`...). Los datos de prueba incluyen 3 casos:

| Localizador | Pasajero | Trayecto | Personas |
|---|---|---|---|
| LOC-0001 | María Fernández | Madrid → Paris | 6 |
| LOC-0002 | José Rodríguez | Barcelona → Roma | 3 |
| LOC-0003 | David Jiménez | Valencia → Nueva York | 2 |

## Menú

```
1. Cargar CSV de reservas
2. Crear ficheros por país de destino
3. Listar ficheros generados
4. Consultar fichero (total)
5. Consultar fichero (línea a línea)
6. Listar reservas ordenadas
7. Buscar reserva por id
8. Buscar reservas por localizador
0. Salir
```

## Diagrama de flujo

![Diagrama de flujo](docs/diagrama_flujo.drawio.png)

El original editable está en [`docs/diagrama_flujo.drawio`](docs/diagrama_flujo.drawio) (abrir con [draw.io](https://app.diagrams.net)).

```mermaid
flowchart TD
    A([Inicio]) --> M[Menú principal]
    M -->|1| C[Cargar CSV y validar líneas]
    CSV[(AirTortilla_Reservas_2020.csv)] --> C
    C --> L[Asignar localizadores]
    L --> R[Lista de reservas en memoria]
    R -->|2| F{Filtro por país destino + fecha}
    F --> S1[(AirTortilla_ES_2020_03_15.csv)]
    F --> S2[(AirTortilla_US_2020_12_20.csv)]
    S1 & S2 -->|3, 4, 5| Q[Consultar: total / línea a línea]
    R -->|6, 7, 8| O[Ordenar · buscar por id · buscar por localizador]
    C & F & Q & O --> FB[Feedback de la operación]
    FB --> M
    M -->|0| Z([Salir])
```

## Arquitectura

```
src/main/java/com/airtortilla
├── Main.java                     Conecta las piezas
├── model/Reserva.java            Entidad (Comparable por id)
├── repository/
│   ├── ReservaRepository.java    Interfaz de acceso a datos
│   └── CsvReservaRepository.java Implementación con CSV
├── service/
│   ├── ReservaService.java       Carga, localizadores, ordenar, buscar
│   ├── ExportadorPorPais.java    Genera los ficheros por país
│   └── ConsultaFicheros.java     Lectura total y línea a línea
├── util/
│   ├── MergeSort.java            Ordenación genérica O(n log n)
│   └── BusquedaBinaria.java      Búsqueda O(log n)
└── ui/ConsolaUI.java             Interfaz de terminal
```

Por capas: la interfaz solo habla con los servicios, y los servicios no saben de dónde vienen los datos.
Para migrar a otra fuente (por ejemplo una base de datos con JDBC) basta con añadir otra
implementación de `ReservaRepository`, sin tocar el resto. Para otra interfaz (web, JavaFX), basta con
sustituir `ConsolaUI`.

## Complejidad algorítmica

| Operación | Algoritmo | Coste |
|---|---|---|
| Cargar CSV | Un recorrido + `HashSet` para ids duplicados | O(n) |
| Asignar localizadores | Agrupar con `Map` por (nombre, origen, destino) | O(n) (comparar todos con todos sería O(n²)) |
| Generar ficheros | Agrupar con `TreeMap` y escribir | O(n log k), k = nº de ficheros |
| Listar ordenadas | MergeSort propio (estable, sirve para objetos) | O(n log n) |
| Buscar por id | Búsqueda binaria sobre la lista ordenada por id | O(log n) |
| Buscar por localizador | Recorrido lineal | O(n) |

## Historial de versiones

| Versión | Fecha | Cambios |
|---|---|---|
| 1.0.0 | 2026-10-14 | Primera entrega: lectura de CSV, localizadores, ficheros por país de destino, consulta total y línea a línea, ordenación y búsqueda. Stack: Java 21 + consola + ficheros. |
