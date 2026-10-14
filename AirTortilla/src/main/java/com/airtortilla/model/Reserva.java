package com.airtortilla.model;

import java.time.LocalDate;

/**
 * Entidad principal: una reserva de vuelo de AirTortilla.
 * Se identifica de forma inequívoca por su id.
 */
public class Reserva implements Comparable<Reserva> {

    private final String id;
    private final String nombrePasajero;
    private final String origen;
    private final String paisOrigen;
    private final String destino;
    private final String paisDestino;
    private final LocalDate fechaReserva;
    private final String numeroVuelo;
    private String localizador; // solo se asigna si 2+ pasajeros coinciden en nombre, origen y destino

    /**
     * Crea una reserva sin localizador.
     *
     * @param id             identificador único de la reserva
     * @param nombrePasajero nombre del pasajero
     * @param origen         ciudad de origen
     * @param paisOrigen     código ISO de 2 letras del país de origen
     * @param destino        ciudad de destino
     * @param paisDestino    código ISO de 2 letras del país de destino
     * @param fechaReserva   fecha en que se hizo la reserva
     * @param numeroVuelo    número de vuelo
     */
    public Reserva(String id, String nombrePasajero, String origen, String paisOrigen,
                   String destino, String paisDestino, LocalDate fechaReserva, String numeroVuelo) {
        this.id = id;
        this.nombrePasajero = nombrePasajero;
        this.origen = origen;
        this.paisOrigen = paisOrigen;
        this.destino = destino;
        this.paisDestino = paisDestino;
        this.fechaReserva = fechaReserva;
        this.numeroVuelo = numeroVuelo;
        this.localizador = "";
    }

    // Getters

    public String getId() { return id; }
    public String getNombrePasajero() { return nombrePasajero; }
    public String getOrigen() { return origen; }
    public String getPaisOrigen() { return paisOrigen; }
    public String getDestino() { return destino; }
    public String getPaisDestino() { return paisDestino; }
    public LocalDate getFechaReserva() { return fechaReserva; }
    public String getNumeroVuelo() { return numeroVuelo; }
    public String getLocalizador() { return localizador; }

    /** @param localizador código del grupo al que pertenece la reserva (por ejemplo LOC-0001) */
    public void setLocalizador(String localizador) { this.localizador = localizador; }

    /** Compara dos reservas por id (orden natural). */
    @Override
    public int compareTo(Reserva otra) {
        return this.id.compareTo(otra.id);
    }

    /** Devuelve la reserva en una línea con columnas alineadas, para mostrarla en consola. */
    @Override
    public String toString() {
        return String.format("%-6s | %-18s | %-10s (%s) -> %-10s (%s) | %s | %-7s | %s",
                id, nombrePasajero, origen, paisOrigen, destino, paisDestino,
                fechaReserva, numeroVuelo, localizador.isEmpty() ? "-" : localizador);
    }
}
