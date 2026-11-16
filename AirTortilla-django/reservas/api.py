"""
API REST para la web 3D (T9). Mismas URLs y mismo JSON que el ReservaController
de la versión Java, pero leyendo de la base de datos con servicios.py.
Todas las rutas exigen sesión iniciada (LoginRequiredMiddleware, US1).
"""
import io

from django.http import JsonResponse
from django.views.decorators.http import require_GET, require_POST

from reservas import servicios
from reservas.models import Reserva

ORDENES = {'ID': 'id', 'FECHA': 'fecha', 'PASAJERO': 'pasajero', 'PAIS_DESTINO': 'pais_destino'}

# Resultado de la última carga desde la web (nombre del fichero y líneas descartadas)
ultima_carga = {'fichero': '', 'errores': []}


def _error(mensaje, estado=400):
    """Respuesta de error con el mismo formato que la versión Java: {"error": "..."}."""
    return JsonResponse({'error': mensaje}, status=estado)


def _reserva_json(r, localizador_de):
    """Convierte una reserva de la BD al JSON que espera la web."""
    return {
        'id': r.codigo,
        'nombrePasajero': ', '.join(p.nombre for p in r.pasajeros.all()),
        'origen': r.origen.nombre,
        'paisOrigen': r.origen.pais.nombre,
        'destino': r.destino.nombre,
        'paisDestino': r.destino.pais.nombre,
        'fechaReserva': r.fecha_reserva.isoformat(),
        'numeroVuelo': r.num_vuelo,
        'localizador': localizador_de.get(r.codigo, ''),
    }


def _localizador_de(grupos):
    """Diccionario código de reserva → localizador."""
    return {r.codigo: loc for loc, reservas in grupos.items() for r in reservas}


def _con_relaciones(consulta):
    """Trae en la misma consulta las ciudades, países y pasajeros (evita cientos de consultas)."""
    return consulta.select_related('origen__pais', 'destino__pais').prefetch_related('pasajeros')


# ---------------------------------------------------------------
# Carga
# ---------------------------------------------------------------

def _estado_json():
    """Contadores de la página y errores de la última carga."""
    total = Reserva.objects.count()
    return JsonResponse({
        'fichero': ultima_carga['fichero'] or ('base de datos' if total else ''),
        'reservas': total,
        'localizadores': len(servicios.grupos_localizador()),
        'errores': ultima_carga['errores'],
        'ficheros': len(servicios.listar_ficheros()),
    })


@require_GET
def estado(request):
    """GET /api/estado: contadores y errores de la última carga."""
    return _estado_json()


@require_POST
def cargar_defecto(request):
    """POST /api/cargar/defecto: importa el CSV de AirTortilla/data."""
    ruta = servicios.CSV_POR_DEFECTO
    if not ruta.exists():
        return _error(f'No existe {ruta}. Arrastra un CSV a la página.')
    with open(ruta, encoding='utf-8-sig', newline='') as f:
        _, errores = servicios.importar_csv(f)
    ultima_carga.update(fichero=ruta.name, errores=errores)
    return _estado_json()


@require_POST
def cargar(request):
    """POST /api/cargar: importa un CSV subido desde el navegador."""
    subido = request.FILES.get('fichero')
    if not subido or subido.size == 0:
        return _error('El fichero está vacío.')
    try:
        texto = subido.read().decode('utf-8-sig')
    except UnicodeDecodeError:
        return _error('El fichero no está en UTF-8.')
    _, errores = servicios.importar_csv(io.StringIO(texto, newline=''))
    ultima_carga.update(fichero=subido.name, errores=errores)
    return _estado_json()


# ---------------------------------------------------------------
# Reservas
# ---------------------------------------------------------------

@require_GET
def reservas(request):
    """GET /api/reservas?orden=FECHA: todas las reservas ordenadas por la BD."""
    orden = request.GET.get('orden', 'ID').upper()
    if orden not in ORDENES:
        return _error(f'Orden no válido: {orden}.')
    localizador_de = _localizador_de(servicios.grupos_localizador())
    consulta = _con_relaciones(servicios.reservas_ordenadas(ORDENES[orden]))
    return JsonResponse([_reserva_json(r, localizador_de) for r in consulta], safe=False)


@require_GET
def reserva(request, codigo):
    """GET /api/reservas/R0005: una reserva por su código."""
    r = servicios.buscar_por_codigo(codigo)
    if r is None:
        return _error(f'No existe ninguna reserva con id {codigo}.', 404)
    return JsonResponse(_reserva_json(r, _localizador_de(servicios.grupos_localizador())))


@require_GET
def localizadores(request):
    """GET /api/localizadores: todos los grupos con localizador."""
    grupos = servicios.grupos_localizador()
    localizador_de = _localizador_de(grupos)
    resultado = []
    for loc, lista in grupos.items():
        filas = [_reserva_json(r, localizador_de) for r in lista]
        primera = filas[0]
        resultado.append({
            'localizador': loc,
            'pasajero': primera['nombrePasajero'],
            'origen': primera['origen'],
            'destino': primera['destino'],
            'reservas': filas,
        })
    return JsonResponse(resultado, safe=False)


# ---------------------------------------------------------------
# Ficheros por país
# ---------------------------------------------------------------

def _info_fichero(ruta):
    """Nombre, país, fecha y nº de reservas de un fichero generado."""
    # AirTortilla_FR_2020_03_15.csv → FR y 2020-03-15
    partes = ruta.stem.split('_')
    return {
        'nombre': ruta.name,
        'pais': partes[1] if len(partes) > 1 else '??',
        'fecha': '-'.join(partes[2:5]) if len(partes) > 4 else '',
        'reservas': len(servicios.leer_fichero(ruta)) - 1,  # sin la cabecera
    }


def _ficheros_json():
    """Lista de ficheros generados con su información."""
    return JsonResponse([_info_fichero(r) for r in servicios.listar_ficheros()], safe=False)


@require_POST
def generar_ficheros(request):
    """POST /api/ficheros/generar: crea los ficheros por país de destino."""
    if not Reserva.objects.exists():
        return _error('Primero carga un CSV de reservas.')
    servicios.exportar_por_pais()
    return _ficheros_json()


@require_GET
def ficheros(request):
    """GET /api/ficheros: lista de ficheros generados."""
    return _ficheros_json()


def _buscar_fichero(nombre):
    """Solo acepta ficheros de la lista, para que no se pueda leer cualquier archivo del disco."""
    return next((r for r in servicios.listar_ficheros() if r.name == nombre), None)


@require_GET
def fichero_completo(request, nombre):
    """GET /api/ficheros/{nombre}: modalidad total, el fichero completo."""
    ruta = _buscar_fichero(nombre)
    if ruta is None:
        return _error(f'No existe el fichero {nombre}.', 404)
    return JsonResponse(servicios.leer_fichero(ruta), safe=False)


@require_GET
def fichero_linea(request, nombre, n):
    """GET /api/ficheros/{nombre}/lineas/{n}: modalidad línea a línea, solo la línea n (desde 0)."""
    ruta = _buscar_fichero(nombre)
    if ruta is None:
        return _error(f'No existe el fichero {nombre}.', 404)
    with open(ruta, encoding='utf-8') as f:
        texto = None
        for _ in range(n + 1):
            texto = f.readline()
            if not texto:
                return JsonResponse({'numero': n, 'texto': '', 'fin': True})
        es_ultima = f.readline() == ''
    return JsonResponse({'numero': n, 'texto': texto.rstrip('\r\n'), 'fin': es_ultima})
