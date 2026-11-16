from django.core.management.base import BaseCommand

from reservas import servicios
from reservas.models import Reserva

CRITERIOS = ['id', 'fecha', 'pasajero', 'pais_destino']


class Command(BaseCommand):
    help = 'Menú de terminal de AirTortilla (el mismo 0-8 de la versión Java), sobre la base de datos'

    def handle(self, *args, **options):
        self.ruta_csv = str(servicios.CSV_POR_DEFECTO)
        self.stdout.write('=== AirTortilla Booking System ===')
        opciones = {
            '1': self.cargar_csv,
            '2': self.generar_ficheros,
            '3': self.listar_ficheros,
            '4': lambda: self.consultar_fichero(total=True),
            '5': lambda: self.consultar_fichero(total=False),
            '6': self.listar_ordenadas,
            '7': self.buscar_por_id,
            '8': self.buscar_por_localizador,
        }
        while True:
            self.mostrar_menu()
            try:
                opcion = input().strip()
            except EOFError:
                break
            if opcion == '0':
                break
            accion = opciones.get(opcion)
            if accion is None:
                self.stdout.write('Opción no válida.')
                continue
            try:
                accion()
            except OSError as e:
                self.stdout.write(f'[ERROR] {e}')
        self.stdout.write('¡Hasta luego!')

    def mostrar_menu(self):
        """Muestra el estado actual y las opciones del menú."""
        total = Reserva.objects.count()
        estado = f'({total} reservas en la base de datos)' if total else '(base de datos vacía)'
        self.stdout.write(f'\nFichero de entrada: {self.ruta_csv}  {estado}')
        self.stdout.write('1. Cargar CSV de reservas')
        self.stdout.write('2. Crear ficheros por país de destino')
        self.stdout.write('3. Listar ficheros generados')
        self.stdout.write('4. Consultar fichero (total)')
        self.stdout.write('5. Consultar fichero (línea a línea)')
        self.stdout.write('6. Listar reservas ordenadas')
        self.stdout.write('7. Buscar reserva por id')
        self.stdout.write('8. Buscar reservas por localizador')
        self.stdout.write('0. Salir')
        self.stdout.write('> ', ending='')
        self.stdout.flush()

    def hay_datos(self):
        """True si hay reservas en la BD; si no, avisa."""
        if not Reserva.objects.exists():
            self.stdout.write('Primero carga el CSV (opción 1).')
            return False
        return True

    def formatear(self, r, localizador_de):
        """Una reserva en una línea, con el mismo formato que la versión Java."""
        pasajeros = ', '.join(p.nombre for p in r.pasajeros.all())
        loc = localizador_de.get(r.codigo, '-')
        return (f'{r.codigo:<6} | {pasajeros:<18} | {r.origen.nombre:<10} ({r.origen.pais}) -> '
                f'{r.destino.nombre:<10} ({r.destino.pais}) | {r.fecha_reserva} | {r.num_vuelo:<7} | {loc}')

    def localizador_de(self):
        """Diccionario código de reserva → localizador."""
        return {r.codigo: loc for loc, lista in servicios.grupos_localizador().items() for r in lista}

    def pedir_numero(self, texto):
        """Pide un número; devuelve None si no es válido."""
        try:
            return int(input(texto).strip())
        except ValueError:
            return None

    # Opción 1
    def cargar_csv(self):
        ruta = input(f'Ruta del CSV [Enter = {self.ruta_csv}]: ').strip()
        if ruta:
            self.ruta_csv = ruta
        try:
            with open(self.ruta_csv, encoding='utf-8-sig', newline='') as f:
                correctas, errores = servicios.importar_csv(f)
        except FileNotFoundError:
            self.stdout.write(f'[ERROR] No existe el fichero {self.ruta_csv}')
            return
        self.stdout.write(f'[OK] Reservas leídas: {correctas}')
        self.stdout.write(f'     Grupos con localizador: {len(servicios.grupos_localizador())}')
        self.stdout.write(f'     Líneas descartadas: {len(errores)}')
        for e in errores:
            self.stdout.write(f'       - {e}')

    # Opción 2
    def generar_ficheros(self):
        if not self.hay_datos():
            return
        creados = servicios.exportar_por_pais()
        self.stdout.write(f'[OK] {len(creados)} ficheros creados en {servicios.CARPETA_SALIDA}')
        for ruta in creados:
            self.stdout.write(f'  - {ruta.name}')

    # Opción 3
    def listar_ficheros(self):
        ficheros = servicios.listar_ficheros()
        if not ficheros:
            self.stdout.write('No hay ficheros generados (opción 2).')
        for i, ruta in enumerate(ficheros, start=1):
            self.stdout.write(f'{i:3d}. {ruta.name}')
        return ficheros

    # Opciones 4 y 5
    def consultar_fichero(self, total):
        ficheros = self.listar_ficheros()
        if not ficheros:
            return
        n = self.pedir_numero('Número de fichero: ')
        if n is None or not 1 <= n <= len(ficheros):
            self.stdout.write('Número no válido.')
            return
        ruta = ficheros[n - 1]
        self.stdout.write(f'--- {ruta.name} ---')
        if total:
            for linea in servicios.leer_fichero(ruta):
                self.stdout.write(linea)
        else:
            self.stdout.write('(Enter = siguiente línea, q = salir)')
            with open(ruta, encoding='utf-8') as f:
                for linea in f:  # se lee de una línea en una, sin cargar el fichero entero
                    self.stdout.write(linea.rstrip('\r\n'), ending='')
                    self.stdout.flush()
                    if input().strip().lower() == 'q':
                        break
        self.stdout.write('--- fin ---')

    # Opción 6
    def listar_ordenadas(self):
        if not self.hay_datos():
            return
        for i, criterio in enumerate(CRITERIOS, start=1):
            self.stdout.write(f"{i}. Por {criterio.replace('_', ' ')}")
        n = self.pedir_numero('Criterio: ')
        if n is None or not 1 <= n <= len(CRITERIOS):
            self.stdout.write('Criterio no válido.')
            return
        localizador_de = self.localizador_de()
        consulta = servicios.reservas_ordenadas(CRITERIOS[n - 1]).select_related(
            'origen__pais', 'destino__pais').prefetch_related('pasajeros')
        for r in consulta:
            self.stdout.write(self.formatear(r, localizador_de))

    # Opción 7
    def buscar_por_id(self):
        if not self.hay_datos():
            return
        r = servicios.buscar_por_codigo(input('Id de la reserva: '))
        if r is None:
            self.stdout.write('No existe ninguna reserva con ese id.')
        else:
            self.stdout.write(self.formatear(r, self.localizador_de()))

    # Opción 8
    def buscar_por_localizador(self):
        if not self.hay_datos():
            return
        grupo = servicios.buscar_por_localizador(input('Localizador (p. ej. LOC-0001): '))
        if not grupo:
            self.stdout.write('No hay reservas con ese localizador.')
        localizador_de = self.localizador_de()
        for r in grupo:
            self.stdout.write(self.formatear(r, localizador_de))
