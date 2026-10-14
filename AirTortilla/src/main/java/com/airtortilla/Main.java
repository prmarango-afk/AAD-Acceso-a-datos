package com.airtortilla;

import com.airtortilla.service.ConsultaFicheros;
import com.airtortilla.service.ExportadorPorPais;
import com.airtortilla.service.ReservaService;
import com.airtortilla.ui.ConsolaUI;
import com.airtortilla.ui.VentanaPrincipal;
import com.airtortilla.web.AirTortillaWebApp;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Punto de entrada. Elige la interfaz según los argumentos:
 * <ul>
 *   <li>(sin argumentos) menú para elegir entre terminal y web</li>
 *   <li>{@code --web} interfaz web en http://localhost:8080</li>
 *   <li>{@code --swing} ventana de escritorio</li>
 *   <li>{@code --consola} menú de terminal</li>
 * </ul>
 * Swing y consola aceptan además: [ficheroEntrada] [carpetaSalida]
 */
public class Main {

    /**
     * Arranca la aplicación con la interfaz elegida.
     *
     * @param args opcional: --swing | --consola, [ficheroEntrada] [carpetaSalida]
     */
    public static void main(String[] args) {
        List<String> lista = Arrays.asList(args);
        if (lista.contains("--consola")) {
            arrancarEscritorio(args, true);
        } else if (lista.contains("--swing")) {
            arrancarEscritorio(args, false);
        } else if (lista.contains("--web")) {
            AirTortillaWebApp.arrancar(args);
        } else if (elegirVersion() == 1) {
            arrancarEscritorio(args, true);
        } else {
            AirTortillaWebApp.arrancar(args);
        }
    }

    /**
     * Pregunta qué versión arrancar antes de empezar.
     *
     * @return 1 para la terminal, 2 para la web
     */
    private static int elegirVersion() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════════════════╗");
        System.out.println("  ║               AirTortilla Booking System · AAD               ║");
        System.out.println("  ╠══════════════════════════════════════════════════════════════╣");
        System.out.println("  ║  1. Versión clásica: terminal                                ║");
        System.out.println("  ║     (sosa, pero cumple todo el enunciado)                    ║");
        System.out.println("  ║                                                              ║");
        System.out.println("  ║  2. Versión para atrevidos: web 3D                           ║");
        System.out.println("  ║     (devoradora de tokens · frontend hecho con IA)           ║");
        System.out.println("  ╚══════════════════════════════════════════════════════════════╝");
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("  Elige versión (1/2): ");
            if (!sc.hasNextLine()) return 2;
            String opcion = sc.nextLine().trim();
            if (opcion.equals("1")) return 1;
            if (opcion.equals("2")) {
                System.out.println("  Despegando… se abrirá el navegador en http://localhost:8080");
                return 2;
            }
            System.out.println("  Opción no válida.");
        }
    }

    /** Arranca la consola o la ventana Swing, sin servidor web. */
    private static void arrancarEscritorio(String[] args, boolean consola) {
        List<String> rutas = new ArrayList<>();
        for (String arg : args) {
            if (!arg.startsWith("--")) rutas.add(arg);
        }
        Path entrada = Path.of(rutas.size() > 0 ? rutas.get(0) : "data/AirTortilla_Reservas_2020.csv");
        Path salida = Path.of(rutas.size() > 1 ? rutas.get(1) : "output");

        ReservaService servicio = new ReservaService();
        ExportadorPorPais exportador = new ExportadorPorPais(salida);
        ConsultaFicheros consulta = new ConsultaFicheros(salida);

        if (consola) {
            new ConsolaUI(servicio, exportador, consulta, entrada).iniciar();
            return;
        }

        // Swing debe crearse en su propio hilo de eventos
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); // aspecto de Windows
            } catch (Exception ignorada) {
                // si falla, se usa el aspecto por defecto de Java
            }
            new VentanaPrincipal(servicio, exportador, consulta, entrada).setVisible(true);
        });
    }
}
