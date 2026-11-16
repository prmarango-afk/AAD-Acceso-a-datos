from django.conf import settings
from django.db.models import Count, Min

from reservas.models import Reserva

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
