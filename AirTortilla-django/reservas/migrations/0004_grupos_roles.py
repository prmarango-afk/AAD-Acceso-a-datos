from django.contrib.auth.management import create_permissions
from django.db import migrations

# Permisos de cada rol (US2). Cada nivel incluye los del anterior.
AGENTE = [
    'reservas.view_reserva', 'reservas.add_reserva', 'reservas.change_reserva',
    'reservas.view_pasajero', 'reservas.add_pasajero', 'reservas.change_pasajero',
    'reservas.view_ciudad', 'reservas.view_pais',
]
SUPERVISOR = AGENTE + [
    'reservas.delete_reserva', 'reservas.delete_pasajero',
    'reservas.add_ciudad', 'reservas.change_ciudad', 'reservas.delete_ciudad',
    'reservas.add_pais', 'reservas.change_pais', 'reservas.delete_pais',
]
ADMINISTRADOR = SUPERVISOR + [
    'auth.view_user', 'auth.add_user', 'auth.change_user', 'auth.delete_user',
    'auth.view_group',
]

ROLES = {
    'Agente': AGENTE,
    'Supervisor': SUPERVISOR,
    'Administrador de sistemas': ADMINISTRADOR,
}


def crear_grupos(apps, schema_editor):
    """Crea los tres grupos de roles con sus permisos."""
    # En una BD nueva los permisos aún no existen: se crean aquí antes de usarlos
    for app_config in apps.get_app_configs():
        app_config.models_module = True
        create_permissions(app_config, verbosity=0)
        app_config.models_module = None

    Group = apps.get_model('auth', 'Group')
    Permission = apps.get_model('auth', 'Permission')
    for nombre, permisos in ROLES.items():
        grupo, _ = Group.objects.get_or_create(name=nombre)
        for permiso in permisos:
            app_label, codename = permiso.split('.')
            grupo.permissions.add(
                Permission.objects.get(content_type__app_label=app_label, codename=codename)
            )


def borrar_grupos(apps, schema_editor):
    """Deshace la migración: borra los tres grupos."""
    apps.get_model('auth', 'Group').objects.filter(name__in=ROLES).delete()


class Migration(migrations.Migration):

    dependencies = [
        ('reservas', '0003_alter_ciudad_options_alter_pais_options_and_more'),
        ('auth', '0012_alter_user_first_name_max_length'),
    ]

    operations = [
        migrations.RunPython(crear_grupos, borrar_grupos),
    ]
