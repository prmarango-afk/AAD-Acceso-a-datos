from django.apps import AppConfig


class ReservasConfig(AppConfig):
    name = 'reservas'

    def ready(self):
        """Conecta las señales que registran los inicios de sesión (US3)."""
        from reservas import signals  # noqa: F401
