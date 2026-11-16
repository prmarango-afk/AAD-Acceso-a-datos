from django.urls import path

from reservas import api, views

# Mismas rutas que el ReservaController de la versión Java
urlpatterns = [
    path('', views.inicio, name='inicio'),
    path('api/estado', api.estado),
    path('api/cargar/defecto', api.cargar_defecto),
    path('api/cargar', api.cargar),
    path('api/reservas', api.reservas),
    path('api/reservas/<str:codigo>', api.reserva),
    path('api/localizadores', api.localizadores),
    path('api/ficheros/generar', api.generar_ficheros),
    path('api/ficheros', api.ficheros),
    path('api/ficheros/<str:nombre>', api.fichero_completo),
    path('api/ficheros/<str:nombre>/lineas/<int:n>', api.fichero_linea),
]
