from django.conf import settings
from django.db import models
from simple_history.models import HistoricalRecords

# Create your models here.

class Pais(models.Model):
    nombre = models.CharField(max_length=100, unique=True)
    history = HistoricalRecords()

    class Meta:
        verbose_name_plural = 'países'

    def __str__(self):
        return self.nombre


class Ciudad(models.Model):
    nombre = models.CharField(max_length=100)
    pais = models.ForeignKey(Pais, on_delete=models.PROTECT)
    history = HistoricalRecords()


    class Meta:
        constraints = [models.UniqueConstraint(fields=['nombre', 'pais'], name='ciudad_unica_por_pais')]
        verbose_name_plural = 'ciudades'

    def __str__(self):
        return self.nombre

class Pasajero(models.Model):
    nombre = models.CharField(max_length=100, unique=True)
    history = HistoricalRecords()

    def __str__(self):
        return self.nombre

class Reserva(models.Model):
    codigo = models.CharField(max_length=10, unique=True)
    num_vuelo = models.CharField(max_length=16)
    fecha_reserva = models.DateField()
    origen = models.ForeignKey(Ciudad, on_delete=models.PROTECT, related_name='reservas_origen')
    destino = models.ForeignKey(Ciudad, on_delete=models.PROTECT, related_name='reservas_destino')
    pasajeros = models.ManyToManyField(Pasajero, related_name='reservas')
    history = HistoricalRecords(m2m_fields=[pasajeros])

    def __str__(self):
        return self.codigo


class RegistroAcceso(models.Model):
    """Inicios de sesión, cierres e intentos fallidos (US3)."""

    class Tipo(models.TextChoices):
        ENTRADA = 'entrada', 'Inicio de sesión'
        SALIDA = 'salida', 'Cierre de sesión'
        FALLIDO = 'fallido', 'Intento fallido'

    fecha = models.DateTimeField(auto_now_add=True)
    tipo = models.CharField(max_length=10, choices=Tipo.choices)
    usuario = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True, on_delete=models.SET_NULL)
    nombre_usuario = models.CharField(max_length=150)
    ip = models.GenericIPAddressField(null=True, blank=True)

    class Meta:
        ordering = ['-fecha']
        verbose_name = 'registro de acceso'
        verbose_name_plural = 'registros de acceso'

    def __str__(self):
        return f'{self.fecha:%Y-%m-%d %H:%M} · {self.get_tipo_display()} · {self.nombre_usuario}'
