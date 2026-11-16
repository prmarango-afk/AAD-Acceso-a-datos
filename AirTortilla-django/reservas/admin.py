from django.contrib import admin
from simple_history.admin import SimpleHistoryAdmin

from .models import Pais, Ciudad, Pasajero, Reserva, RegistroAcceso

# Register your models here.
# SimpleHistoryAdmin añade el botón «History» en cada ficha (US3)

@admin.register(Pais)
class PaisAdmin(SimpleHistoryAdmin):
    search_fields = ['nombre']


@admin.register(Ciudad)
class CiudadAdmin(SimpleHistoryAdmin):
    list_display = ['nombre', 'pais']
    list_filter = ['pais']
    search_fields = ['nombre']


@admin.register(Pasajero)
class PasajeroAdmin(SimpleHistoryAdmin):
    search_fields = ['nombre']


@admin.register(Reserva)
class ReservaAdmin(SimpleHistoryAdmin):
    list_display = ['codigo', 'num_vuelo', 'fecha_reserva', 'origen', 'destino']
    list_filter = ['fecha_reserva', 'destino__pais']
    search_fields = ['codigo', 'num_vuelo', 'pasajeros__nombre']
    filter_horizontal = ['pasajeros']


@admin.register(RegistroAcceso)
class RegistroAccesoAdmin(admin.ModelAdmin):
    """Solo lectura: el registro de accesos no se puede tocar a mano."""
    list_display = ['fecha', 'tipo', 'nombre_usuario', 'ip']
    list_filter = ['tipo', 'fecha']
    search_fields = ['nombre_usuario']

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False
