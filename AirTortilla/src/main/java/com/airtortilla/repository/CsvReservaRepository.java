package com.airtortilla.repository;

import com.airtortilla.model.Reserva;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lee las reservas de un fichero CSV separado por ';' con la cabecera {@link #CABECERA}.
 */
public class CsvReservaRepository implements ReservaRepository {

    public static final String SEPARADOR = ";";
    public static final String CABECERA =
            "IdReserva;NombrePasajero;Origen;PaisOrigen;Destino;PaisDestino;FechaReserva;NumeroVuelo";
    private static final int NUM_COLUMNAS = 8;

    private final Path fichero;
    private final List<String> errores = new ArrayList<>();

    /** @param fichero ruta del CSV de entrada */
    public CsvReservaRepository(Path fichero) {
        this.fichero = fichero;
    }

    /**
     * Lee el CSV línea a línea. Las líneas incorrectas o con id repetido se descartan
     * y se guardan en la lista de errores.
     *
     * @return reservas válidas, en el orden del fichero
     * @throws IOException si no se puede abrir o leer el fichero
     */
    @Override
    public List<Reserva> findAll() throws IOException {
        errores.clear();
        List<Reserva> reservas = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        try (BufferedReader br = Files.newBufferedReader(fichero, StandardCharsets.UTF_8)) {
            String linea = br.readLine(); // cabecera
            int numLinea = 1;
            while ((linea = br.readLine()) != null) {
                numLinea++;
                if (linea.isBlank()) continue;
                try {
                    Reserva r = parsear(linea);
                    if (!ids.add(r.getId())) {
                        errores.add("Línea " + numLinea + ": id duplicado " + r.getId());
                        continue;
                    }
                    reservas.add(r);
                } catch (IllegalArgumentException | DateTimeParseException e) {
                    errores.add("Línea " + numLinea + ": " + e.getMessage());
                }
            }
        }
        return reservas;
    }

    /**
     * Convierte una línea del CSV en una Reserva, validando sus campos.
     *
     * @param linea línea del CSV (sin la cabecera)
     * @return la reserva creada
     * @throws IllegalArgumentException si faltan columnas, hay campos vacíos o el país no es ISO
     * @throws DateTimeParseException   si la fecha no tiene el formato yyyy-MM-dd
     */
    private Reserva parsear(String linea) {
        String[] c = linea.split(SEPARADOR, -1);
        if (c.length != NUM_COLUMNAS) {
            throw new IllegalArgumentException("se esperaban " + NUM_COLUMNAS + " columnas y hay " + c.length);
        }
        for (int i = 0; i < c.length; i++) {
            c[i] = c[i].trim();
            if (c[i].isEmpty()) throw new IllegalArgumentException("columna " + (i + 1) + " vacía");
        }
        String paisDestino = c[5].toUpperCase();
        if (!paisDestino.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("PaisDestino debe ser un código ISO de 2 letras: " + c[5]);
        }
        return new Reserva(c[0], c[1], c[2], c[3].toUpperCase(), c[4], paisDestino,
                LocalDate.parse(c[6]), c[7]);
    }

    /** @return errores de la última carga, con su número de línea */
    @Override
    public List<String> getErrores() {
        return errores;
    }
}
