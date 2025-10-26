# PYMERP Flutter Client

## Requisitos
- Flutter 3.22+
- Android SDK, Chrome, Windows Desktop

* BASE_URL (ej: http://localhost:8081)
`ash
cd app_flutter
make bootstrap
make gen
make run-web
`

### Variables de entorno (--dart-define)

* BASE_URL (ej: http://localhost:8080)
* COMPANY_ID (ej: dev-company)
* USE_HTTPS (true/false)

### Endpoints usados

* GET /v1/customers?page=&size=
* POST /v1/customers
* PUT /v1/customers/{id}
* DELETE /v1/customers/{id}

### Funcionalidades clave

* Clientes con paginación/infinite scroll y refresco manual.
* Formularios con lat/lng opcionales (double?), validaciones de rango y botón **“Usar ubicación actual”**.
* Serialización JSON omitiendo lat/lng cuando son 
ull.
* Cabeceras Authorization y X-Company-Id en todas las peticiones (multitenencia).

### Testing

`ash
make test
`
Incluye pruebas de serialización y controladores de paginación; se añadió un widget test para validar la forma de clientes.

### Generación de modelos

Ejecuta el generador cuando modifiques clases Freezed/JSON serializable:

```bash
flutter pub run build_runner build --delete-conflicting-outputs
```

### Notas

* Para geolocalización en Web/Android/Windows se solicita permiso en tiempo de ejecución; si se deniega, se informa mediante SnackBar.
* Falta implementar edición de clientes, sincronización offline y manejo avanzado de errores.
