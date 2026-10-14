package com.airtortilla.service;

import com.airtortilla.model.Reserva;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Divide las reservas en ficheros por país de destino y fecha de reserva.
 * Nombre de cada fichero: AirTortilla_XX_YYYY_MM_DD.csv
 */
public class ExportadorPorPais {

    public static final String PREFIJO = "AirTortilla_";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy_MM_dd");
    private static final String CABECERA_SALIDA =
            "IdReserva;NombrePasajero;Origen;PaisOrigen;Destino;PaisDestino;FechaReserva;NumeroVuelo;Localizador";

    private final Path carpetaSalida;

    /** @param carpetaSalida carpeta donde se escriben los ficheros */
    public ExportadorPorPais(Path carpetaSalida) {
        this.carpetaSalida = carpetaSalida;
    }

    /** @return carpeta donde se escriben los ficheros */
    public Path getCarpetaSalida() { return carpetaSalida; }

    /**
     * Crea un fichero por cada combinación de país de destino y fecha de reserva.
     * Si un fichero ya existe, se sobrescribe.
     *
     * @param reservas reservas a exportar
     * @return rutas de los ficheros creados
     * @throws IOException si no se puede crear la carpeta o escribir algún fichero
     */
    public List<Path> exportar(List<Reserva> reservas) throws IOException {
        Files.createDirectories(carpetaSalida);

        // Agrupa las reservas por nombre de fichero (ordenados alfabéticamente)
        Map<String, List<Reserva>> porFichero = new TreeMap<>();
        for (Reserva r : reservas) {
            String nombre = PREFIJO + r.getPaisDestino() + "_" + r.getFechaReserva().format(FORMATO_FECHA) + ".csv";
            porFichero.computeIfAbsent(nombre, k -> new ArrayList<>()).add(r);
        }

        // Escribe cada grupo en su fichero
        List<Path> creados = new ArrayList<>();
        for (Map.Entry<String, List<Reserva>> entrada : porFichero.entrySet()) {
            Path destino = carpetaSalida.resolve(entrada.getKey());
            try (BufferedWriter bw = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
                bw.write(CABECERA_SALIDA);
                bw.newLine();
                for (Reserva r : entrada.getValue()) {
                    bw.write(String.join(";", r.getId(), r.getNombrePasajero(), r.getOrigen(), r.getPaisOrigen(),
                            r.getDestino(), r.getPaisDestino(), r.getFechaReserva().toString(),
                            r.getNumeroVuelo(), r.getLocalizador()));
                    bw.newLine();
                }
            }
            creados.add(destino);
        }
        return creados;
    }
}
