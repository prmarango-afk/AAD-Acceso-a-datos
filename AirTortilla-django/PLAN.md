# Plan · AirTortilla Entrega 2 (migración a base de datos)

> Documento de trabajo para seguir el proyecto desde cualquier ordenador.
> Se actualiza al terminar cada tarea (marcar `[x]` y apuntar la fecha).

## 1. Qué pide el enunciado

Enunciado completo: [`docs/enunciado_entrega2.pdf`](docs/enunciado_entrega2.pdf)

**User Stories**
- **US1 · Login**: como Empleado quiero poder hacer log in para acceder a los recursos de la compañía.
- **US2 · Alta de usuarios**: como Administrador de Sistemas quiero poder dar de alta usuarios para garantizar la seguridad interna.
- **US3 · Trazabilidad**: como Administrador de Sistemas quiero mantener la trazabilidad de las modificaciones.

**Además**
- **Obligatorio**: migrar los datos (el CSV) a una **base de datos relacional**.
- **Deseable** (no crítico): contenedores para facilitar el despliegue (Docker).
- **Entregable**: convertir las User Stories y la tarea en un **conjunto de tareas medibles** (sección 3).
- **Aviso**: es la **última entrega** en la que se permite cambiar de stack sin documentarlo como apartado obligatorio.

## 2. Decisiones tomadas

| Decisión | Elección | Motivo |
|---|---|---|
| Stack | **Python 3.14 + Django 6.1** | Lo usamos en el trabajo; trae login, usuarios y panel de administración. Se aprovecha la última ventana para cambiar de stack. |
| Base de datos | **SQLite** en local · **PostgreSQL** en "producción" (Docker, opcional) | Igual que en el trabajo. Django cambia de una a otra solo con la configuración. |
| Versión Java | Se queda en `AirTortilla/` sin tocar (v2.0.0) | Historial de la migración; el README principal lo explica. |
| Proyecto nuevo | Carpeta `AirTortilla-django/`, proyecto `config`, app `reservas` | Convención habitual de Django. |
| Frontend | Reutilizar la web 3D de la v2.0 | Si la API de Django mantiene las mismas URLs, la página funciona sin cambios. |
| Trazabilidad (US3) | `django-simple-history` *(pendiente de confirmar)* | Estándar en la industria; menos tiempo que hacerlo a mano con señales. |
| Forma de trabajar | **Mixta**: modelos, importación, consultas y lógica → lo escribo yo con guía. Configuración repetitiva, adaptar la web, Docker y documentación → Claude, explicándolo. | Aprender lo importante sin perder horas en lo accesorio. |

## 3. Tareas medibles

| # | Tarea | Criterio de aceptación (hecha cuando…) | Estado |
|---|---|---|---|
| T1 | Entorno y proyecto Django | `runserver` arranca sin errores, `requirements.txt` en UTF-8 y `migrate` aplicado | [x] 2026-11-16 |
| T2 | Diseño del modelo relacional | Diagrama entidad-relación en `docs/` con tablas, claves y relaciones | [x] 2026-11-16 |
| T3 | Modelos y migraciones | `migrate` crea las tablas y se ven en el panel de administración | [x] 2026-11-16 |
| T4 | Importar el CSV a la BD | `python manage.py importar_csv` carga las 71 reservas, informa de las líneas erróneas y no duplica si se ejecuta dos veces | [x] 2026-11-16 |
| T5 | Lógica sobre la BD | Localizadores, ordenación, búsqueda y ficheros por país funcionan leyendo de la BD | [x] 2026-11-16 |
| T6 | **US1 · Login** | Sin sesión iniciada no se accede a nada: web y API redirigen al login | [ ] |
| T7 | **US2 · Alta de usuarios** | Un administrador crea un empleado desde el panel y ese empleado puede entrar | [ ] |
| T8 | **US3 · Trazabilidad** | Cada alta, cambio o borrado de una reserva queda registrado con usuario, fecha y valores anteriores; también los inicios de sesión | [ ] |
| T9 | API + web 3D | La web de la v2.0 funciona igual, pero sobre Django y con login | [ ] |
| T10 | Versión terminal | `python manage.py airtortilla` muestra el menú de siempre (0-8) | [ ] |
| T11 | Docker *(opcional)* | `docker compose up` arranca Django + PostgreSQL | [ ] |
| T12 | Documentación | README con tareas, diagrama E-R, migración Java → Django e instrucciones | [ ] |

## 4. Estado actual

**Hecho (2026-11-02)**
- Entorno virtual `.venv` con Python 3.14 y Django 6.1.2 instalado.
- Proyecto `config` y app `reservas` creados; `reservas` añadida a `INSTALLED_APPS`.
- `TIME_ZONE = 'Europe/Madrid'` (el idioma se deja en inglés a propósito).
- `.gitignore` de la raíz: ignora `.venv/`, `__pycache__/`, `*.sqlite3` y `.env`.

- `requirements.txt` generado (en UTF-8).

**Hecho (2026-11-16, portátil)**
- T1: `migrate` aplicado y `runserver` sin errores.
- T2: diagrama E-R en `docs/diagrama_er.png` (hecho con dbdiagram.io).
  5 tablas: `pais`, `ciudad` (FK país), `pasajero`, `reserva` (FK origen y destino → ciudad) y la intermedia reserva↔pasajero.
  **Decisión**: Reserva–Pasajero es **N:M** aunque el CSV trae un pasajero por reserva, porque una reserva real puede llevar varios pasajeros.
- T3: modelos en `reservas/models.py` (migraciones `0001` y `0002`) y registrados en `admin.py`; se ven en `/admin`. Superusuario creado en el portátil.
  - `Reserva.localizador` guarda el `IdReserva` del CSV (`R0001`), único → sirve para no duplicar al importar.
  - `Ciudad`: única la pareja nombre + país (`UniqueConstraint`), no el nombre solo.
  - El DNI y otros datos del pasajero se dejan para más adelante (el CSV no los trae).

- Plurales del admin corregidos (`países`, `ciudades`).
- T4: comando `python manage.py importar_csv` (`reservas/management/commands/importar_csv.py`).
  - `get_or_create` para país, ciudad y pasajero; `update_or_create` por `localizador` para la reserva → no duplica.
  - `try/except` por línea: informa de las erróneas (número de línea y motivo) y sigue.
  - Resultado: 71 reservas, 22 pasajeros, 14 ciudades, 8 países. Ejecutado dos veces → siguen 71.

**En el PC de casa**: `git pull`, copiar el CSV a `AirTortilla/data/`, `migrate`, `createsuperuser` e `importar_csv` (la BD no se sube).

- `Reserva.localizador` renombrado a `codigo` (migración `0003`, `RenameField`): en la versión Java «localizador» es otra cosa (`LOC-0001`).
- T5: `reservas/servicios.py` (equivale a `ReservaService` + `ExportadorPorPais` + `ConsultaFicheros` de Java):
  - `reservas_ordenadas(criterio)`: `id`, `fecha`, `pasajero`, `pais_destino` con `order_by`.
  - `buscar_por_codigo(codigo)`: sin distinguir mayúsculas; `None` si no existe.
  - `grupos_localizador()` / `buscar_por_localizador()`: se calculan con `GROUP BY` (no se guardan). Salen 3: LOC-0001 a LOC-0003.
  - `exportar_por_pais()`, `listar_ficheros()`, `leer_fichero()`: escribe en `AirTortilla-django/output/`. Comprobado: 27 ficheros idénticos a los de la versión Java.

**Siguiente paso**: T6, login (US1).

## 5. Ponerlo en marcha en otro ordenador

Desde la carpeta del repo (`AAD - Acceso a datos`):

```powershell
git pull
cd AirTortilla-django
py -3.14 -m venv .venv                       # solo la primera vez en ese ordenador
.\.venv\Scripts\Activate.ps1                 # cada vez que abras una terminal
python -m pip install -r requirements.txt    # solo la primera vez o si cambia
python manage.py migrate                     # crea/actualiza la BD local
python manage.py runserver                   # http://127.0.0.1:8000
```

- El `.venv` y la base de datos `db.sqlite3` **no se suben a GitHub**: cada ordenador tiene los suyos.
- El CSV de datos tampoco está en GitHub: copiar `AirTortilla/data/AirTortilla_Reservas_2020.csv` a mano (USB, Drive…).
- Si PowerShell no deja activar el entorno («ejecución de scripts deshabilitada»):
  `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`

## 6. Recordatorios

- Al empezar: `git pull`. Al acabar: actualizar este plan, `git add .`, `git commit` y `git push`.
- Guardar archivos que lean otros programas con `Out-File -Encoding utf8`, no con `>`.
- Para retomar con Claude en el otro ordenador: pedirle que lea este `PLAN.md`.
