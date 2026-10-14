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

    /** @param carpeta carpeta donde están los ficheros generados */
    public ConsultaFicheros(Path carpeta) {
        this.carpeta = carpeta;
    }

    /**
     * Lista los ficheros de AirTortilla de la carpeta de salida, ordenados por nombre.
     *
     * @return rutas de los ficheros, o lista vacía si la carpeta no existe
     * @throws IOException si no se puede leer la carpeta
     */
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

    /**
     * Modalidad total: lee el fichero completo.
     *
     * @param fichero fichero a leer
     * @return todas sus líneas
     * @throws IOException si no se puede leer el fichero
     */
    public List<String> leerTodo(Path fichero) throws IOException {
        return Files.readAllLines(fichero, StandardCharsets.UTF_8);
    }

    /**
     * Abre un fichero para leerlo poco a poco (lo usa la interfaz gráfica,
     * que pide una línea cada vez que se pulsa "Siguiente línea").
     * Quien lo llame debe cerrarlo al terminar.
     *
     * @param fichero fichero a abrir
     * @return lector del fichero
     * @throws IOException si no se puede abrir el fichero
     */
    public BufferedReader abrir(Path fichero) throws IOException {
        return Files.newBufferedReader(fichero, StandardCharsets.UTF_8);
    }

    /**
     * Modalidad línea a línea: lee el fichero de una línea en una.
     *
     * @param fichero fichero a leer
     * @param alLeer  acción con cada línea; si devuelve false, se deja de leer
     * @throws IOException si no se puede leer el fichero
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
