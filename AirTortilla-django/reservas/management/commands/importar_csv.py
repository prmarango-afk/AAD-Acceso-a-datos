from django.core.management.base import BaseCommand

from reservas.servicios import CSV_POR_DEFECTO, importar_csv


class Command(BaseCommand):
    help = 'Importa las reservas del CSV a la base de datos'

    def add_arguments(self, parser):
        parser.add_argument('ruta', nargs='?', default=str(CSV_POR_DEFECTO), help='CSV a importar')

    def handle(self, *args, **options):
        # La lógica está en servicios.py para que la use también la web (botón «Cargar»)
        with open(options['ruta'], encoding='utf-8-sig', newline='') as f:
            correctas, errores = importar_csv(f)

        for error in errores:
            self.stderr.write(error)
        self.stdout.write(f'Importación terminada: {correctas} correctas, {len(errores)} con errores')
