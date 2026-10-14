package com.airtortilla.ui;

import com.airtortilla.model.Reserva;
import com.airtortilla.repository.CsvReservaRepository;
import com.airtortilla.service.ConsultaFicheros;
import com.airtortilla.service.ExportadorPorPais;
import com.airtortilla.service.ReservaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Interfaz de terminal de AirTortilla Booking System.
 * Solo se encarga de pedir datos y mostrar resultados; la lógica está en los servicios.
 */
public class ConsolaUI {

    private final Scanner sc = new Scanner(System.in);
    private final ReservaService servicio;
    private final ExportadorPorPais exportador;
    private final ConsultaFicheros consulta;
    private Path ficheroEntrada;

    /**
     * @param servicio       gestión de las reservas
     * @param exportador     generador de ficheros por país
     * @param consulta       lectura de los ficheros generados
     * @param ficheroEntrada CSV que se carga por defecto
     */
    public ConsolaUI(ReservaService servicio, ExportadorPorPais exportador,
                     ConsultaFicheros consulta, Path ficheroEntrada) {
        this.servicio = servicio;
        this.exportador = exportador;
        this.consulta = consulta;
        this.ficheroEntrada = ficheroEntrada;
    }

    /** Muestra el menú y ejecuta las opciones elegidas hasta que el usuario sale. */
    public void iniciar() {
        System.out.println("=== AirTortilla Booking System ===");
        boolean salir = false;
        while (!salir) {
            mostrarMenu();
            String opcion = sc.nextLine().trim();
            try {
                switch (opcion) {
                    case "1" -> cargarCsv();
                    case "2" -> generarFicheros();
                    case "3" -> listarFicheros();
                    case "4" -> consultarFichero(true);
                    case "5" -> consultarFichero(false);
                    case "6" -> listarOrdenadas();
                    case "7" -> buscarPorId();
                    case "8" -> buscarPorLocalizador();
                    case "0" -> salir = true;
                    default -> System.out.println("Opción no válida.");
                }
            } catch (IOException e) {
                System.out.println("[ERROR] " + e.getMessage());
            }
        }
        System.out.println("¡Hasta luego!");
    }

    /** Muestra el estado actual y las opciones del menú. */
    private void mostrarMenu() {
        System.out.println();
        System.out.println("Fichero de entrada: " + ficheroEntrada
                + (servicio.hayDatos() ? "  (" + servicio.getReservas().size() + " reservas cargadas)" : "  (sin cargar)"));
        System.out.println("1. Cargar CSV de reservas");
        System.out.println("2. Crear ficheros por país de destino");
        System.out.println("3. Listar ficheros generados");
        System.out.println("4. Consultar fichero (total)");
        System.out.println("5. Consultar fichero (línea a línea)");
        System.out.println("6. Listar reservas ordenadas");
        System.out.println("7. Buscar reserva por id");
        System.out.println("8. Buscar reservas por localizador");
        System.out.println("0. Salir");
        System.out.print("> ");
    }

    /** Opción 1: pide la ruta del CSV, lo carga y muestra el resumen de la carga. */
    private void cargarCsv() throws IOException {
        System.out.print("Ruta del CSV [Enter = " + ficheroEntrada + "]: ");
        String ruta = sc.nextLine().trim();
        if (!ruta.isEmpty()) ficheroEntrada = Path.of(ruta);
        if (!Files.exists(ficheroEntrada)) {
            System.out.println("[ERROR] No existe el fichero " + ficheroEntrada.toAbsolutePath());
            return;
        }
        servicio.cargar(new CsvReservaRepository(ficheroEntrada));

        // Feedback de la operación
        System.out.println("[OK] Reservas leídas: " + servicio.getReservas().size());
        System.out.println("     Grupos con localizador: " + servicio.getGruposConLocalizador());
        List<String> errores = servicio.getErroresCarga();
        System.out.println("     Líneas descartadas: " + errores.size());
        errores.forEach(e -> System.out.println("       - " + e));
    }

    /** @return true si hay reservas cargadas; si no, avisa al usuario */
    private boolean comprobarDatos() {
        if (!servicio.hayDatos()) {
            System.out.println("Primero carga el CSV (opción 1).");
            return false;
        }
        return true;
    }

    /** Opción 2: genera los ficheros por país de destino y muestra los creados. */
    private void generarFicheros() throws IOException {
        if (!comprobarDatos()) return;
        List<Path> creados = exportador.exportar(servicio.getReservas());
        System.out.println("[OK] " + creados.size() + " ficheros creados en "
                + exportador.getCarpetaSalida().toAbsolutePath());
        creados.forEach(p -> System.out.println("  - " + p.getFileName()));
    }

    /**
     * Opción 3: muestra los ficheros generados numerados.
     *
     * @return los ficheros mostrados
     */
    private List<Path> listarFicheros() throws IOException {
        List<Path> ficheros = consulta.listar();
        if (ficheros.isEmpty()) {
            System.out.println("No hay ficheros generados (opción 2).");
        }
        for (int i = 0; i < ficheros.size(); i++) {
            System.out.printf("%3d. %s%n", i + 1, ficheros.get(i).getFileName());
        }
        return ficheros;
    }

    /**
     * Opciones 4 y 5: pide un fichero y lo muestra entero o línea a línea.
     *
     * @param total true para mostrarlo entero, false para línea a línea
     */
    private void consultarFichero(boolean total) throws IOException {
        List<Path> ficheros = listarFicheros();
        if (ficheros.isEmpty()) return;
        System.out.print("Número de fichero: ");
        int n;
        try {
            n = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Número no válido.");
            return;
        }
        if (n < 1 || n > ficheros.size()) {
            System.out.println("Número fuera de rango.");
            return;
        }
        Path fichero = ficheros.get(n - 1);
        System.out.println("--- " + fichero.getFileName() + " ---");
        if (total) {
            consulta.leerTodo(fichero).forEach(System.out::println);
        } else {
            System.out.println("(Enter = siguiente línea, q = salir)");
            consulta.leerLineaALinea(fichero, linea -> {
                System.out.print(linea);
                return !sc.nextLine().trim().equalsIgnoreCase("q");
            });
        }
        System.out.println("--- fin ---");
    }

    /** Opción 6: pide un criterio y muestra las reservas ordenadas. */
    private void listarOrdenadas() {
        if (!comprobarDatos()) return;
        ReservaService.Criterio[] criterios = ReservaService.Criterio.values();
        for (int i = 0; i < criterios.length; i++) {
            System.out.println((i + 1) + ". Por " + criterios[i].name().toLowerCase().replace('_', ' '));
        }
        System.out.print("Criterio: ");
        int n;
        try {
            n = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            n = -1;
        }
        if (n < 1 || n > criterios.length) {
            System.out.println("Criterio no válido.");
            return;
        }
        servicio.ordenar(criterios[n - 1]).forEach(System.out::println);
    }

    /** Opción 7: pide un id y muestra la reserva. */
    private void buscarPorId() {
        if (!comprobarDatos()) return;
        System.out.print("Id de la reserva: ");
        Optional<Reserva> r = servicio.buscarPorId(sc.nextLine().trim());
        System.out.println(r.map(Reserva::toString).orElse("No existe ninguna reserva con ese id."));
    }

    /** Opción 8: pide un localizador y muestra todas las reservas del grupo. */
    private void buscarPorLocalizador() {
        if (!comprobarDatos()) return;
        System.out.print("Localizador (p. ej. LOC-0001): ");
        List<Reserva> grupo = servicio.buscarPorLocalizador(sc.nextLine().trim());
        if (grupo.isEmpty()) System.out.println("No hay reservas con ese localizador.");
        grupo.forEach(System.out::println);
    }
}
