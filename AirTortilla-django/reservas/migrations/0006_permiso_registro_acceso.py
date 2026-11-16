from django.contrib.auth.management import create_permissions
from django.db import migrations


def dar_permiso(apps, schema_editor):
    """El Administrador de sistemas puede consultar el registro de accesos (US3)."""
    for app_config in apps.get_app_configs():
        app_config.models_module = True
        create_permissions(app_config, verbosity=0)
        app_config.models_module = None

    Group = apps.get_model('auth', 'Group')
    Permission = apps.get_model('auth', 'Permission')
    grupo = Group.objects.get(name='Administrador de sistemas')
    grupo.permissions.add(
        Permission.objects.get(content_type__app_label='reservas', codename='view_registroacceso')
    )


def quitar_permiso(apps, schema_editor):
    """Deshace la migración."""
    Group = apps.get_model('auth', 'Group')
    Permission = apps.get_model('auth', 'Permission')
    grupo = Group.objects.filter(name='Administrador de sistemas').first()
    if grupo:
        grupo.permissions.remove(
            *Permission.objects.filter(content_type__app_label='reservas', codename='view_registroacceso')
        )


class Migration(migrations.Migration):

    dependencies = [
        ('reservas', '0005_trazabilidad'),
    ]

    operations = [
        migrations.RunPython(dar_permiso, quitar_permiso),
    ]
