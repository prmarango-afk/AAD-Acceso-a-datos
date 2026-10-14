package com.airtortilla.repository;

import com.airtortilla.model.Reserva;

import java.io.IOException;
import java.util.List;

/**
 * Fuente de datos de las reservas.
 * Hoy se implementa con CSV; en futuras entregas se puede añadir otra
 * implementación (por ejemplo JDBC) sin tocar el servicio ni la interfaz.
 */
public interface ReservaRepository {

    List<Reserva> findAll() throws IOException;

    /** Errores encontrados en la última carga (líneas mal formadas, ids repetidos...). */
    List<String> getErrores();
}
