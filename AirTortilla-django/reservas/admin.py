from django.contrib import admin
from .models import Pais, Ciudad, Pasajero, Reserva

# Register your models here.

admin.site.register(Pais)
admin.site.register(Ciudad)
admin.site.register(Pasajero)
admin.site.register(Reserva)