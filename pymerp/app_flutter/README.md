# PYMERP Flutter Client

## Requisitos
- Flutter 3.22+
- Android SDK, Chrome, Windows Desktop

## Setup r�pido
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

* Clientes con paginaci�n/infinite scroll y refresco manual.
* Formularios con lat/lng opcionales (double?), validaciones de rango y bot�n **�Usar ubicaci�n actual�**.
* Serializaci�n JSON omitiendo lat/lng cuando son 
ull.
* Cabeceras Authorization y X-Company-Id en todas las peticiones (multitenencia).

### Testing

`ash
make test
`
Incluye pruebas de serializaci�n y controladores de paginaci�n; se a�adi� un widget test para validar la forma de clientes.

### Notas

* Para geolocalizaci�n en Web/Android/Windows se solicita permiso en tiempo de ejecuci�n; si se deniega, se informa mediante SnackBar.
* Falta implementar edici�n de clientes, sincronizaci�n offline y manejo avanzado de errores.
