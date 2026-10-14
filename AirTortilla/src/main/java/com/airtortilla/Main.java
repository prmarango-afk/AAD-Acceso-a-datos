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

/**
 * Punto de entrada. Elige la interfaz según los argumentos:
 * <ul>
 *   <li>(sin argumentos) interfaz web en http://localhost:8080</li>
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
        } else {
            AirTortillaWebApp.arrancar(args);
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
