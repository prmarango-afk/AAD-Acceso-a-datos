#!/bin/sh
# Se ejecuta cada vez que arranca el contenedor web
set -e

echo "Aplicando migraciones..."
python manage.py migrate --noinput

echo "Juntando los estáticos..."
python manage.py collectstatic --noinput

# Crea el superusuario la primera vez (si ya existe, no hace nada)
if [ -n "$DJANGO_SUPERUSER_USERNAME" ]; then
    python manage.py createsuperuser --noinput 2>/dev/null || echo "El superusuario ya existe."
fi

# Importa el CSV si está montado (no duplica si se repite)
if [ -f /AirTortilla/data/AirTortilla_Reservas_2020.csv ]; then
    python manage.py importar_csv
fi

echo "Arrancando AirTortilla en http://localhost:8000"
exec gunicorn config.wsgi:application --bind 0.0.0.0:8000 --workers 2
