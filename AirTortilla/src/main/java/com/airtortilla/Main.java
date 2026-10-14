package com.airtortilla;

import com.airtortilla.service.ConsultaFicheros;
import com.airtortilla.service.ExportadorPorPais;
import com.airtortilla.service.ReservaService;
import com.airtortilla.ui.ConsolaUI;

import java.nio.file.Path;

/**
 * Punto de entrada: solo crea las piezas y las conecta.
 * Uso opcional: java -jar AirTortilla.jar [ficheroEntrada] [carpetaSalida]
 */
public class Main {

    /**
     * Arranca la aplicación de consola.
     *
     * @param args opcional: [ficheroEntrada] [carpetaSalida]
     */
    public static void main(String[] args) {
        Path entrada = Path.of(args.length > 0 ? args[0] : "data/AirTortilla_Reservas_2020.csv");
        Path salida = Path.of(args.length > 1 ? args[1] : "output");

        new ConsolaUI(new ReservaService(), new ExportadorPorPais(salida),
                new ConsultaFicheros(salida), entrada).iniciar();
    }
}
