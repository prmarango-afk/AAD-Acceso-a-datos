package com.airtortilla.web;

import com.airtortilla.model.Reserva;
import com.airtortilla.repository.CsvReservaRepository;
import com.airtortilla.service.ConsultaFicheros;
import com.airtortilla.service.ExportadorPorPais;
import com.airtortilla.service.ReservaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * API REST que usa la página web. Cada método responde a una URL y devuelve JSON.
 * No tiene lógica de negocio: llama a los mismos servicios que Swing y la consola.
 */
@RestController
@RequestMapping("/api")
public class ReservaController {

    /** Resumen del estado actual, para los contadores de la página. */
    public record Estado(String fichero, int reservas, int localizadores, List<String> errores, int ficheros) { }

    /** Un grupo de reservas con el mismo localizador. */
    public record Grupo(String localizador, String pasajero, String origen, String destino, List<Reserva> reservas) { }

    /** Datos de un fichero generado. */
    public record InfoFichero(String nombre, String pais, String fecha, int reservas) { }

    /** Una línea de un fichero en la lectura línea a línea. */
    public record Linea(int numero, String texto, boolean fin) { }

    private final ReservaService servicio;
    private final ExportadorPorPais exportador;
    private final ConsultaFicheros consulta;
    private final Path entradaPorDefecto;
    private String ficheroCargado = "";

    /** Spring pasa aquí los servicios creados en {@link AirTortillaWebApp}. */
    public ReservaController(ReservaService servicio, ExportadorPorPais exportador, ConsultaFicheros consulta,
                             @Value("${airtortilla.entrada}") String entrada) {
        this.servicio = servicio;
        this.exportador = exportador;
        this.consulta = consulta;
        this.entradaPorDefecto = Path.of(entrada);
    }

    // ---------------------------------------------------------------
    // Carga
    // ---------------------------------------------------------------

    /** GET /api/estado: contadores y errores de la última carga. */
    @GetMapping("/estado")
    public Estado estado() throws IOException {
        return new Estado(ficheroCargado, servicio.getReservas().size(), servicio.getGruposConLocalizador(),
                servicio.getErroresCarga(), consulta.listar().size());
    }

    /** POST /api/cargar/defecto: carga el CSV configurado en application.properties. */
    @PostMapping("/cargar/defecto")
    public Estado cargarPorDefecto() throws IOException {
        if (!Files.exists(entradaPorDefecto)) {
            throw new IllegalArgumentException("No existe " + entradaPorDefecto.toAbsolutePath()
                    + ". Arrastra un CSV a la página.");
        }
        servicio.cargar(new CsvReservaRepository(entradaPorDefecto));
        ficheroCargado = entradaPorDefecto.getFileName().toString();
        return estado();
    }

    /** POST /api/cargar: carga un CSV subido desde el navegador. */
    @PostMapping("/cargar")
    public Estado cargarSubido(@RequestParam("fichero") MultipartFile fichero) throws IOException {
        if (fichero.isEmpty()) throw new IllegalArgumentException("El fichero está vacío.");
        // El repositorio lee de disco, así que se guarda en un fichero temporal
        Path temporal = Files.createTempFile("airtortilla-", ".csv");
        try {
            fichero.transferTo(temporal);
            servicio.cargar(new CsvReservaRepository(temporal));
        } finally {
            Files.deleteIfExists(temporal);
        }
        ficheroCargado = fichero.getOriginalFilename();
        return estado();
    }

    // ---------------------------------------------------------------
    // Reservas
    // ---------------------------------------------------------------

    /** GET /api/reservas?orden=FECHA: todas las reservas ordenadas con MergeSort. */
    @GetMapping("/reservas")
    public List<Reserva> reservas(@RequestParam(defaultValue = "ID") ReservaService.Criterio orden) {
        return servicio.ordenar(orden);
    }

    /** GET /api/reservas/R0005: una reserva por id (búsqueda binaria). */
    @GetMapping("/reservas/{id}")
    public Reserva reserva(@PathVariable String id) {
        return servicio.buscarPorId(id.trim().toUpperCase())
                .orElseThrow(() -> new NoSuchElementException("No existe ninguna reserva con id " + id + "."));
    }

    /** GET /api/localizadores: todos los grupos con localizador. */
    @GetMapping("/localizadores")
    public List<Grupo> localizadores() {
        List<Grupo> grupos = new ArrayList<>();
        for (Map.Entry<String, List<Reserva>> e : servicio.getGruposLocalizador().entrySet()) {
            Reserva primera = e.getValue().get(0);
            grupos.add(new Grupo(e.getKey(), primera.getNombrePasajero(), primera.getOrigen(),
                    primera.getDestino(), e.getValue()));
        }
        return grupos;
    }

    // ---------------------------------------------------------------
    // Ficheros por país
    // ---------------------------------------------------------------

    /** POST /api/ficheros/generar: crea los ficheros por país de destino. */
    @PostMapping("/ficheros/generar")
    public List<InfoFichero> generar() throws IOException {
        if (!servicio.hayDatos()) throw new IllegalArgumentException("Primero carga un CSV de reservas.");
        exportador.exportar(servicio.getReservas());
        return ficheros();
    }

    /** GET /api/ficheros: lista de ficheros generados con su país, fecha y nº de reservas. */
    @GetMapping("/ficheros")
    public List<InfoFichero> ficheros() throws IOException {
        List<InfoFichero> info = new ArrayList<>();
        for (Path p : consulta.listar()) {
            String nombre = p.getFileName().toString();
            // AirTortilla_FR_2020_03_15.csv → FR y 2020-03-15
            String[] partes = nombre.replace(".csv", "").split("_");
            String pais = partes.length > 1 ? partes[1] : "??";
            String fecha = partes.length > 4 ? partes[2] + "-" + partes[3] + "-" + partes[4] : "";
            int reservas = consulta.leerTodo(p).size() - 1; // sin la cabecera
            info.add(new InfoFichero(nombre, pais, fecha, reservas));
        }
        return info;
    }

    /** GET /api/ficheros/{nombre}: modalidad total, el fichero completo. */
    @GetMapping("/ficheros/{nombre}")
    public List<String> ficheroCompleto(@PathVariable String nombre) throws IOException {
        return consulta.leerTodo(buscarFichero(nombre));
    }

    /** GET /api/ficheros/{nombre}/lineas/{n}: modalidad línea a línea, solo la línea n (desde 0). */
    @GetMapping("/ficheros/{nombre}/lineas/{n}")
    public Linea linea(@PathVariable String nombre, @PathVariable int n) throws IOException {
        try (BufferedReader br = consulta.abrir(buscarFichero(nombre))) {
            String texto = null;
            for (int i = 0; i <= n; i++) {
                texto = br.readLine();
                if (texto == null) return new Linea(n, "", true);
            }
            boolean esUltima = br.readLine() == null;
            return new Linea(n, texto, esUltima);
        }
    }

    /**
     * Devuelve la ruta de un fichero generado a partir de su nombre.
     * Solo acepta ficheros de la lista, para que no se pueda leer cualquier archivo del disco.
     */
    private Path buscarFichero(String nombre) throws IOException {
        return consulta.listar().stream()
                .filter(p -> p.getFileName().toString().equals(nombre))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No existe el fichero " + nombre + "."));
    }

    // ---------------------------------------------------------------
    // Errores → respuesta JSON con un mensaje claro
    // ---------------------------------------------------------------

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> peticionIncorrecta(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> errorFichero(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error de lectura/escritura: " + e.getMessage()));
    }
}
