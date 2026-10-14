package com.airtortilla.service;

import com.airtortilla.model.Reserva;
import com.airtortilla.repository.ReservaRepository;
import com.airtortilla.util.BusquedaBinaria;
import com.airtortilla.util.MergeSort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gestiona las reservas cargadas en memoria: carga, localizadores, ordenación y búsqueda.
 */
public class ReservaService {

    /** Criterios por los que se pueden ordenar las reservas. */
    public enum Criterio {
        ID(Comparator.comparing(Reserva::getId)),
        FECHA(Comparator.comparing(Reserva::getFechaReserva).thenComparing(Reserva::getId)),
        PASAJERO(Comparator.comparing(Reserva::getNombrePasajero).thenComparing(Reserva::getId)),
        PAIS_DESTINO(Comparator.comparing(Reserva::getPaisDestino).thenComparing(Reserva::getId));

        private final Comparator<Reserva> comparador;

        Criterio(Comparator<Reserva> comparador) { this.comparador = comparador; }

        public Comparator<Reserva> comparador() { return comparador; }
    }

    private List<Reserva> reservas = new ArrayList<>();
    private List<Reserva> ordenadasPorId = new ArrayList<>(); // necesaria para la búsqueda binaria
    private List<String> erroresCarga = new ArrayList<>();
    private int gruposConLocalizador;

    /**
     * Carga las reservas desde el repositorio, asigna los localizadores
     * y prepara la lista ordenada por id para las búsquedas.
     *
     * @param repositorio origen de los datos (CSV, base de datos...)
     * @throws IOException si no se puede leer el origen de datos
     */
    public void cargar(ReservaRepository repositorio) throws IOException {
        reservas = repositorio.findAll();
        erroresCarga = new ArrayList<>(repositorio.getErrores());
        gruposConLocalizador = asignarLocalizadores();
        ordenadasPorId = MergeSort.ordenar(reservas, Criterio.ID.comparador());
    }

    /**
     * Asigna el mismo localizador a las reservas que coinciden en nombre,
     * origen y destino (grupos de 2 o más). Coste O(n).
     *
     * @return número de grupos con localizador
     */
    private int asignarLocalizadores() {
        // Agrupa las reservas por nombre + origen + destino
        Map<String, List<Reserva>> grupos = new LinkedHashMap<>();
        for (Reserva r : reservas) {
            String clave = (r.getNombrePasajero() + "|" + r.getOrigen() + "|" + r.getDestino()).toLowerCase();
            grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(r);
        }

        // Los grupos con 2 o más reservas reciben un localizador LOC-0001, LOC-0002...
        int contador = 0;
        for (List<Reserva> grupo : grupos.values()) {
            if (grupo.size() >= 2) {
                contador++;
                String localizador = String.format("LOC-%04d", contador);
                for (Reserva r : grupo) {
                    r.setLocalizador(localizador);
                }
            }
        }
        return contador;
    }

    /** @return todas las reservas en el orden del fichero de entrada */
    public List<Reserva> getReservas() { return reservas; }

    /** @return mensajes de las líneas descartadas en la última carga */
    public List<String> getErroresCarga() { return erroresCarga; }

    /** @return número de grupos con localizador */
    public int getGruposConLocalizador() { return gruposConLocalizador; }

    /** @return true si hay reservas cargadas */
    public boolean hayDatos() { return !reservas.isEmpty(); }

    /**
     * Devuelve las reservas ordenadas según el criterio indicado, sin modificar la lista original.
     * Usa MergeSort: O(n log n).
     *
     * @param criterio criterio de ordenación
     * @return nueva lista ordenada
     */
    public List<Reserva> ordenar(Criterio criterio) {
        return MergeSort.ordenar(reservas, criterio.comparador());
    }

    /**
     * Busca una reserva por su id mediante búsqueda binaria: O(log n).
     *
     * @param id id de la reserva
     * @return la reserva, o vacío si no existe
     */
    public Optional<Reserva> buscarPorId(String id) {
        int pos = BusquedaBinaria.buscar(ordenadasPorId, id, Reserva::getId);
        if (pos >= 0) {
            return Optional.of(ordenadasPorId.get(pos));
        }
        return Optional.empty();
    }

    /**
     * Devuelve todas las reservas que comparten un localizador: O(n).
     *
     * @param localizador código del localizador (por ejemplo LOC-0001)
     * @return reservas del grupo, o lista vacía si no existe
     */
    public List<Reserva> buscarPorLocalizador(String localizador) {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva r : reservas) {
            if (r.getLocalizador().equalsIgnoreCase(localizador)) {
                resultado.add(r);
            }
        }
        return resultado;
    }
}
