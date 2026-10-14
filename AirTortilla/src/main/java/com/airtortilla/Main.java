package com.airtortilla;

import com.airtortilla.service.ConsultaFicheros;
import com.airtortilla.service.ExportadorPorPais;
import com.airtortilla.service.ReservaService;
import com.airtortilla.ui.ConsolaUI;
import com.airtortilla.ui.VentanaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Punto de entrada: crea las piezas y abre la interfaz elegida.
 * Uso: java -jar AirTortilla.jar [--consola] [ficheroEntrada] [carpetaSalida]
 */
public class Main {

    /**
     * Arranca la aplicación. Por defecto abre la interfaz gráfica.
     *
     * @param args opcional: --consola para usar la terminal, [ficheroEntrada] [carpetaSalida]
     */
    public static void main(String[] args) {
        // Separa la opción --consola de las rutas
        boolean modoConsola = false;
        List<String> rutas = new ArrayList<>();
        for (String arg : args) {
            if (arg.equals("--consola")) modoConsola = true;
            else rutas.add(arg);
        }

        Path entrada = Path.of(rutas.size() > 0 ? rutas.get(0) : "data/AirTortilla_Reservas_2020.csv");
        Path salida = Path.of(rutas.size() > 1 ? rutas.get(1) : "output");

        ReservaService servicio = new ReservaService();
        ExportadorPorPais exportador = new ExportadorPorPais(salida);
        ConsultaFicheros consulta = new ConsultaFicheros(salida);

        if (modoConsola) {
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
