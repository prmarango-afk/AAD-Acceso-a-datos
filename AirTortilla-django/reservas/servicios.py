import csv
import re
from datetime import date

from django.conf import settings
from django.db import transaction
from django.db.models import Count, Min

from reservas.models import Pais, Ciudad, Pasajero, Reserva

CSV_POR_DEFECTO = settings.BASE_DIR.parent / 'AirTortilla' / 'data' / 'AirTortilla_Reservas_2020.csv'
COLUMNAS = ['IdReserva', 'NombrePasajero', 'Origen', 'PaisOrigen', 'Destino', 'PaisDestino', 'FechaReserva', 'NumeroVuelo']


def importar_csv(fichero):
    """
    Importa al BD las reservas de un CSV ya abierto (texto). No duplica: si la reserva
    ya existe, solo la actualiza si algún dato ha cambiado.
    Devuelve (número de líneas correctas, lista de errores «Línea N: motivo»).
    """
    correctas = 0
    errores = []
    vistos = set()
    lector = csv.DictReader(fichero, delimiter=';')
    for num_linea, fila in enumerate(lector, start=2):
        try:
            with transaction.atomic():  # si la línea falla, no se queda nada a medias
                _importar_fila(fila, vistos)
            correctas += 1
        except Exception as e:
            errores.append(f'Línea {num_linea}: {e}')
    return correctas, errores


def _importar_fila(fila, vistos):
    """Valida una fila del CSV y la guarda en la BD (mismas reglas que la versión Java)."""
    if None in fila or any(fila.get(c) is None for c in COLUMNAS):
        raise ValueError(f'se esperaban {len(COLUMNAS)} columnas')
    datos = {c: fila[c].strip() for c in COLUMNAS}
    for i, c in enumerate(COLUMNAS, start=1):
        if not datos[c]:
            raise ValueError(f'columna {i} vacía')
    for c in ['PaisOrigen', 'PaisDestino']:
        if not re.fullmatch(r'[A-Z]{2}', datos[c]):
            raise ValueError(f'{c} debe ser un código ISO de 2 letras: {datos[c]}')
    fecha = date.fromisoformat(datos['FechaReserva'])
    if datos['IdReserva'] in vistos:
        raise ValueError(f"id duplicado {datos['IdReserva']}")
    vistos.add(datos['IdReserva'])

    pais_origen, _ = Pais.objects.get_or_create(nombre=datos['PaisOrigen'])
    origen, _ = Ciudad.objects.get_or_create(nombre=datos['Origen'], pais=pais_origen)
    pais_destino, _ = Pais.objects.get_or_create(nombre=datos['PaisDestino'])
    destino, _ = Ciudad.objects.get_or_create(nombre=datos['Destino'], pais=pais_destino)
    pasajero, _ = Pasajero.objects.get_or_create(nombre=datos['NombrePasajero'])

    nuevos = {'num_vuelo': datos['NumeroVuelo'], 'fecha_reserva': fecha, 'origen': origen, 'destino': destino}
    reserva = Reserva.objects.filter(codigo=datos['IdReserva']).first()
    if reserva is None:
        reserva = Reserva.objects.create(codigo=datos['IdReserva'], **nuevos)
    elif any(getattr(reserva, campo) != valor for campo, valor in nuevos.items()):
        # Solo se guarda si cambia algo: así el historial (US3) no se llena de cambios vacíos
        for campo, valor in nuevos.items():
            setattr(reserva, campo, valor)
        reserva.save()
    if not reserva.pasajeros.filter(pk=pasajero.pk).exists():
        reserva.pasajeros.add(pasajero)

CRITERIOS = {
    'id': 'codigo',
    'fecha': 'fecha_reserva',
    'pasajero': 'pasajeros__nombre',
    'pais_destino': 'destino__pais__nombre',
}


def reservas_ordenadas(criterio='id'):
    """Devuelve todas las reservas ordenadas por el criterio indicado."""
    campo = CRITERIOS[criterio]
    return Reserva.objects.order_by(campo, 'codigo')


def buscar_por_codigo(codigo):
    """Devuelve la reserva con ese código (R0001...) o None si no existe."""
    return Reserva.objects.filter(codigo__iexact=codigo.strip()).first()


def grupos_localizador():
    """Agrupa las reservas del mismo pasajero, origen y destino (2 o más) y les da un localizador LOC-0001..."""
    grupos = (
        Reserva.objects
        .values('pasajeros', 'origen', 'destino')
        .annotate(total=Count('id'), primera=Min('codigo'))
        .filter(total__gte=2)
        .order_by('primera')
    )
    resultado = {}
    for numero, grupo in enumerate(grupos, start=1):
        localizador = f'LOC-{numero:04d}'
        resultado[localizador] = list(
            Reserva.objects.filter(
                pasajeros=grupo['pasajeros'],
                origen=grupo['origen'],
                destino=grupo['destino'],
            ).order_by('codigo')
        )
    return resultado


def buscar_por_localizador(localizador):
    """Devuelve las reservas de ese localizador (LOC-0001...) o una lista vacía."""
    return grupos_localizador().get(localizador.strip().upper(), [])


CARPETA_SALIDA = settings.BASE_DIR / 'output'
PREFIJO = 'AirTortilla_'
CABECERA_SALIDA = 'IdReserva;NombrePasajero;Origen;PaisOrigen;Destino;PaisDestino;FechaReserva;NumeroVuelo;Localizador'


def exportar_por_pais():
    """Crea un fichero por cada país de destino y fecha (AirTortilla_XX_YYYY_MM_DD.csv) y devuelve sus rutas."""
    CARPETA_SALIDA.mkdir(exist_ok=True)

    localizador_de = {}
    for localizador, reservas in grupos_localizador().items():
        for r in reservas:
            localizador_de[r.codigo] = localizador

    por_fichero = {}
    for r in reservas_ordenadas('id'):
        nombre = f"{PREFIJO}{r.destino.pais.nombre}_{r.fecha_reserva.strftime('%Y_%m_%d')}.csv"
        por_fichero.setdefault(nombre, []).append(r)

    creados = []
    for nombre in sorted(por_fichero):
        ruta = CARPETA_SALIDA / nombre
        with open(ruta, 'w', encoding='utf-8', newline='') as f:
            f.write(CABECERA_SALIDA + '\n')
            for r in por_fichero[nombre]:
                pasajeros = ', '.join(p.nombre for p in r.pasajeros.all())
                f.write(';'.join([
                    r.codigo, pasajeros, r.origen.nombre, r.origen.pais.nombre,
                    r.destino.nombre, r.destino.pais.nombre, r.fecha_reserva.isoformat(),
                    r.num_vuelo, localizador_de.get(r.codigo, ''),
                ]) + '\n')
        creados.append(ruta)
    return creados


def listar_ficheros():
    """Devuelve las rutas de los ficheros generados, ordenadas por nombre."""
    if not CARPETA_SALIDA.is_dir():
        return []
    return sorted(CARPETA_SALIDA.glob(f'{PREFIJO}*.csv'))


def leer_fichero(ruta):
    """Devuelve todas las líneas de un fichero generado."""
    return ruta.read_text(encoding='utf-8').splitlines()
