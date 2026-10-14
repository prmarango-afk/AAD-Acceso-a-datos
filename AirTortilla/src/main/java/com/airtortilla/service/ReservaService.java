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
 * Lógica de negocio sobre las reservas cargadas en memoria.
 * No sabe de dónde vienen los datos (eso es cosa del repositorio).
 */
public class ReservaService {

    /** Criterios de ordenación disponibles. */
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
    private List<Reserva> ordenadasPorId = new ArrayList<>();
    private List<String> erroresCarga = new ArrayList<>();
    private int gruposConLocalizador;

    /** Carga las reservas del repositorio y asigna localizadores. */
    public void cargar(ReservaRepository repositorio) throws IOException {
        reservas = repositorio.findAll();
        erroresCarga = new ArrayList<>(repositorio.getErrores());
        gruposConLocalizador = asignarLocalizadores();
        ordenadasPorId = MergeSort.ordenar(reservas, Criterio.ID.comparador());
    }

    /**
     * Agrupa por (nombre, origen, destino) con un Map: O(n).
     * A cada grupo de 2 o más reservas le asigna el mismo localizador.
     *
     * @return número de grupos con localizador
     */
    private int asignarLocalizadores() {
        Map<String, List<Reserva>> grupos = new LinkedHashMap<>();
        for (Reserva r : reservas) {
            String clave = (r.getNombrePasajero() + "|" + r.getOrigen() + "|" + r.getDestino()).toLowerCase();
            grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(r);
        }
        int contador = 0;
        for (List<Reserva> grupo : grupos.values()) {
            if (grupo.size() >= 2) {
                contador++;
                String localizador = String.format("LOC-%04d", contador);
                for (Reserva r : grupo) r.setLocalizador(localizador);
            }
        }
        return contador;
    }

    public List<Reserva> getReservas() { return reservas; }
    public List<String> getErroresCarga() { return erroresCarga; }
    public int getGruposConLocalizador() { return gruposConLocalizador; }
    public boolean hayDatos() { return !reservas.isEmpty(); }

    public List<Reserva> ordenar(Criterio criterio) {
        return MergeSort.ordenar(reservas, criterio.comparador());
    }

    /** Búsqueda binaria sobre la lista ordenada por id: O(log n). */
    public Optional<Reserva> buscarPorId(String id) {
        int pos = BusquedaBinaria.buscar(ordenadasPorId, id, Reserva::getId);
        return pos >= 0 ? Optional.of(ordenadasPorId.get(pos)) : Optional.empty();
    }

    /** Reservas que comparten un localizador. */
    public List<Reserva> buscarPorLocalizador(String localizador) {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva r : reservas) {
            if (r.getLocalizador().equalsIgnoreCase(localizador)) resultado.add(r);
        }
        return resultado;
    }
}
