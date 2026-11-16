from django.db import models

# Create your models here.

class Pais(models.Model):
    nombre = models.CharField(max_length=100, unique=True)
    
    class Meta:
        verbose_name_plural = 'países'
    
    def __str__(self):
        return self.nombre

    
class Ciudad(models.Model):
    nombre = models.CharField(max_length=100)
    pais = models.ForeignKey(Pais, on_delete=models.PROTECT)
    
    
    class Meta:
        constraints = [models.UniqueConstraint(fields=['nombre', 'pais'], name='ciudad_unica_por_pais')]
        verbose_name_plural = 'ciudades'
    
    def __str__(self):
        return self.nombre

class Pasajero(models.Model):
    nombre = models.CharField(max_length=100, unique=True)
    
    def __str__(self):
        return self.nombre

class Reserva(models.Model):
    localizador = models.CharField(max_length=10, unique=True)
    num_vuelo = models.CharField(max_length=16)
    fecha_reserva = models.DateField()
    origen = models.ForeignKey(Ciudad, on_delete=models.PROTECT, related_name='reservas_origen')
    destino = models.ForeignKey(Ciudad, on_delete=models.PROTECT, related_name='reservas_destino')
    pasajeros = models.ManyToManyField(Pasajero, related_name='reservas')
    
    def __str__(self):
        return self.localizador
  

    
  