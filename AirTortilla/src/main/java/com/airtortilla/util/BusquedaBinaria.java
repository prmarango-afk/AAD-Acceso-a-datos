package com.airtortilla.util;

import java.util.List;
import java.util.function.Function;

/**
 * Búsqueda binaria sobre una lista YA ordenada por la clave buscada. Coste O(log n).
 */
public final class BusquedaBinaria {

    private BusquedaBinaria() { }

    /** Devuelve la posición del elemento cuya clave es igual a la buscada, o -1 si no está. */
    public static <T, K extends Comparable<K>> int buscar(List<T> lista, K clave, Function<T, K> extractor) {
        int inicio = 0;
        int fin = lista.size() - 1;
        while (inicio <= fin) {
            int medio = (inicio + fin) >>> 1;
            int cmp = extractor.apply(lista.get(medio)).compareTo(clave);
            if (cmp == 0) return medio;
            if (cmp < 0) inicio = medio + 1;
            else fin = medio - 1;
        }
        return -1;
    }
}
