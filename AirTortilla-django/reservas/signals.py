from django.contrib.auth.signals import user_logged_in, user_logged_out, user_login_failed
from django.dispatch import receiver

from reservas.models import RegistroAcceso


def _ip(request):
    """IP desde la que se hace la petición (None si no hay petición)."""
    return request.META.get('REMOTE_ADDR') if request else None


@receiver(user_logged_in)
def registrar_entrada(sender, request, user, **kwargs):
    """Apunta cada inicio de sesión."""
    RegistroAcceso.objects.create(tipo=RegistroAcceso.Tipo.ENTRADA, usuario=user,
                                  nombre_usuario=user.get_username(), ip=_ip(request))


@receiver(user_logged_out)
def registrar_salida(sender, request, user, **kwargs):
    """Apunta cada cierre de sesión."""
    if user is not None:
        RegistroAcceso.objects.create(tipo=RegistroAcceso.Tipo.SALIDA, usuario=user,
                                      nombre_usuario=user.get_username(), ip=_ip(request))


@receiver(user_login_failed)
def registrar_fallo(sender, credentials, request=None, **kwargs):
    """Apunta cada intento de inicio de sesión fallido."""
    RegistroAcceso.objects.create(tipo=RegistroAcceso.Tipo.FALLIDO,
                                  nombre_usuario=credentials.get('username', ''), ip=_ip(request))
