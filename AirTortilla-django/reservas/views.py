from django.shortcuts import render


def inicio(request):
    """Portada: la web 3D de la v2.0, que pide los datos a la API (/api/...)."""
    return render(request, 'web/index.html')
