package com.airtortilla.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MergeSort genérico. Coste O(n log n) en todos los casos y estable
 * (mantiene el orden relativo de los elementos iguales).
 */
public final class MergeSort {

    private MergeSort() { }

    public static <T> List<T> ordenar(List<T> lista, Comparator<? super T> criterio) {
        if (lista.size() <= 1) {
            return new ArrayList<>(lista); // caso base: ya está ordenada
        }
        int mitad = lista.size() / 2;
        List<T> izquierda = ordenar(lista.subList(0, mitad), criterio);
        List<T> derecha = ordenar(lista.subList(mitad, lista.size()), criterio);
        return mezclar(izquierda, derecha, criterio);
    }

    private static <T> List<T> mezclar(List<T> a, List<T> b, Comparator<? super T> criterio) {
        List<T> resultado = new ArrayList<>(a.size() + b.size());
        int i = 0, j = 0;
        while (i < a.size() && j < b.size()) {
            if (criterio.compare(a.get(i), b.get(j)) <= 0) {
                resultado.add(a.get(i++));
            } else {
                resultado.add(b.get(j++));
            }
        }
        while (i < a.size()) resultado.add(a.get(i++));
        while (j < b.size()) resultado.add(b.get(j++));
        return resultado;
    }
}
