package com.airtortilla.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Consulta de los ficheros generados: listado, lectura total y lectura línea a línea.
 */
public class ConsultaFicheros {

    private final Path carpeta;

    public ConsultaFicheros(Path carpeta) {
        this.carpeta = carpeta;
    }

    public List<Path> listar() throws IOException {
        List<Path> ficheros = new ArrayList<>();
        if (!Files.isDirectory(carpeta)) return ficheros;
        try (Stream<Path> s = Files.list(carpeta)) {
            s.filter(p -> p.getFileName().toString().startsWith(ExportadorPorPais.PREFIJO))
             .sorted()
             .forEach(ficheros::add);
        }
        return ficheros;
    }

    /** Modalidad total: devuelve todo el contenido de golpe. */
    public List<String> leerTodo(Path fichero) throws IOException {
        return Files.readAllLines(fichero, StandardCharsets.UTF_8);
    }

    /**
     * Modalidad línea a línea: lee una línea cada vez y se la pasa a {@code alLeer}.
     * Si {@code alLeer} devuelve false, se deja de leer.
     */
    public void leerLineaALinea(Path fichero, Predicate<String> alLeer) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(fichero, StandardCharsets.UTF_8)) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (!alLeer.test(linea)) break;
            }
        }
    }
}
