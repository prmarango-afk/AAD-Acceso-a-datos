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

    public String getId() { return id; }
    public String getNombrePasajero() { return nombrePasajero; }
    public String getOrigen() { return origen; }
    public String getPaisOrigen() { return paisOrigen; }
    public String getDestino() { return destino; }
    public String getPaisDestino() { return paisDestino; }
    public LocalDate getFechaReserva() { return fechaReserva; }
    public String getNumeroVuelo() { return numeroVuelo; }
    public String getLocalizador() { return localizador; }

    public void setLocalizador(String localizador) { this.localizador = localizador; }

    /** Orden natural: por id. */
    @Override
    public int compareTo(Reserva otra) {
        return this.id.compareTo(otra.id);
    }

    @Override
    public String toString() {
        return String.format("%-6s | %-18s | %-10s (%s) -> %-10s (%s) | %s | %-7s | %s",
                id, nombrePasajero, origen, paisOrigen, destino, paisDestino,
                fechaReserva, numeroVuelo, localizador.isEmpty() ? "-" : localizador);
    }
}
