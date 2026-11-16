import csv
from django.conf import settings
from django.core.management.base import BaseCommand
from reservas.models import Pais, Ciudad, Pasajero, Reserva
from datetime import date

class Command(BaseCommand):
    help = 'Importa las reservas del CSV a la base de datos'

    def handle(self, *args, **options):
        ruta = settings.BASE_DIR.parent / 'AirTortilla' / 'data' / 'AirTortilla_Reservas_2020.csv'
        correctas = 0
        errores = 0

        with open(ruta, encoding='utf-8-sig', newline='') as f:
            lector = csv.DictReader(f, delimiter=';')
            for num_linea, fila in enumerate(lector, start=2):
                try:
                    pais_origen, _ = Pais.objects.get_or_create(nombre=fila['PaisOrigen'])
                    origen, _ = Ciudad.objects.get_or_create(nombre=fila['Origen'], pais=pais_origen)
                    pais_destino, _ = Pais.objects.get_or_create(nombre=fila['PaisDestino'])
                    destino, _ = Ciudad.objects.get_or_create(nombre=fila['Destino'], pais=pais_destino)
                    pasajero, _ = Pasajero.objects.get_or_create(nombre=fila['NombrePasajero'])
                    reserva, _ = Reserva.objects.update_or_create(localizador=fila['IdReserva'], defaults={
                        'num_vuelo': fila['NumeroVuelo'],
                        'fecha_reserva': date.fromisoformat(fila['FechaReserva']),
                        'origen': origen,
                        'destino': destino,
                    })
                    reserva.pasajeros.add(pasajero)
                    correctas += 1
                except Exception as e:
                    errores += 1
                    self.stderr.write(f'Línea {num_linea}: {e}')

        self.stdout.write(f'Importación terminada: {correctas} correctas, {errores} con errores')
