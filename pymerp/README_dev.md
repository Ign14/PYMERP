# Pruebas rápidas — Backend protegido (Keycloak + Spring Boot)

## Configuración de red

- **Backend**: Se ejecuta en `http://localhost:8081`
- **Frontend**: Se ejecuta en `http://172.29.0.1:5173/app` (configurado para WSL/Docker)
- **Keycloak**: Se ejecuta en `http://localhost:8082`

El backend ya está configurado para aceptar requests CORS desde `http://172.29.0.1:5173`.

## Requisitos de Java (LTS)

- JDK 21 (LTS). El proyecto ya está configurado con Gradle Toolchains para Java 21 (`java.toolchain = 21`), por lo que el propio Gradle puede descargar un JDK compatible automáticamente durante el build.
- Opcional: si prefieres usar tu JDK local, asegúrate de que `JAVA_HOME` apunte a una instalación de Java 21 y que `java -version` muestre 21.x.

No es necesario instalar manualmente otra versión si usas el wrapper (`gradlew.bat` / `./gradlew`), ya que gestionará el toolchain.

Pasos rápidos

1. Copia `.env.sample` a `.env` en la raíz del proyecto y completa valores si es necesario:

   ```powershell
   copy .env.sample .env
   ```

2. Asegúrate de que el backend esté corriendo en `http://localhost:8081` (ejecutado en otra terminal):

   ```powershell
   .\gradlew bootRun --args="--spring.profiles.active=dev"
   ```

3. Desde VS Code puedes usar la extensión REST Client abriendo `requests.http` y ejecutando las peticiones en orden (Get Token -> Health -> Products). El `access_token` será capturado en la variable `access_token`.

4. O usa la Task de VS Code `Test Protected Endpoint` (Ctrl+Shift+P -> Tasks: Run Task -> "Test Protected Endpoint") que ejecuta `scripts/test-protected.ps1`.

Criterios de aceptación

- `GET /actuator/health` → 200 con `{"status":"UP"}` o similar.
- `GET /api/v1/products` → 200 y lista de productos si el token tiene los roles/claims apropiados.
- Si `GET /api/v1/products` responde 401 o 403, el script/report debe imprimir los claims principales del JWT (sub, preferred_username, scope, realm_access.roles, resource_access) y una recomendación precisa de ajuste.

Troubleshooting común

- Puerto incorrecto: asegúrate de que `BACKEND_BASE` en `.env` apunte a `http://localhost:8081`.
- Falta header `X-Company-Id`: este header es obligatorio para multitenancy; el script lo añade desde `COMPANY_ID` en `.env`.
- Mapeo de roles: si recibes 401/403 y en los claims `realm_access.roles` aparece `erp_user` pero la aplicación espera `ROLE_ERP_USER`, ajusta `OidcRoleMapper` para normalizar roles a `ROLE_*` o actualiza `SecurityConfig` para aceptar roles sin prefijo.

Recomendaciones de ajuste (si detectas 401/403)

- Normalizar roles: en `OidcRoleMapper` convertir `realm_access.roles` -> `ROLE_<upper>` y retornar `new SimpleGrantedAuthority("ROLE_...")`.
- O usar scopes: si la app usa scopes, mapear `scope` claim a `SCOPE_...` authorities o ajustar `JwtAuthenticationConverter`.

Reporte automático (qué imprime el script)

- HTTP status y body para `/actuator/health` y `/api/v1/products`.
- Si el endpoint protegido falla (401/403), imprime los claims principales del JWT y una recomendación de ajuste en SecurityConfig/OidcRoleMapper.

Notas finales

- No comitees `.env` con secretos; `.env.sample` contiene placeholders.
- Los scripts son idempotentes y seguros para ejecuciones repetidas en entorno de desarrollo.

## Autorización efectiva

Matriz rápida:

- **200**: token válido que incluye `realm_access.roles` con `erp_user` (mapeado a `ROLE_ERP_USER`) o claim `scope` que contenga `products:read`, y header `X-Company-Id` válido.
- **403**: token válido pero sin roles ni scopes esperados.
- **400**: falta `X-Company-Id` o no es UUID válido.
- **401**: no hay token o el token es inválido/expirado.

Ejemplo (requests.http extra):

### Products con scope explícito (si el token no trae roles)
GET {{BACKEND_BASE}}/api/v1/products
Authorization: Bearer {{access_token}}
X-Company-Id: 00000000-0000-0000-0000-000000000001

