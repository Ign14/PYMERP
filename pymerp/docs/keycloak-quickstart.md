# Keycloak quickstart para PyMEs (local)

Este documento describe cómo levantar un Keycloak local rápido, importar un realm de ejemplo, crear un usuario y obtener tokens para probar la API.

Requisitos
- Docker y docker-compose instalados.

1) Levantar Keycloak (container)

```bash
# from project root (Windows cmd/powershell - adapta comillas si es necesario)
mkdir -p .local/keycloak
cat > .local/keycloak/realm-export.json <<EOF
{
  "realm": "pymerp",
  "enabled": true,
  "clients": [
    {
      "clientId": "pymerp-backend",
      "publicClient": true,
      "redirectUris": ["*"]
    },
    {
      "clientId": "pymerp-frontend",
      "publicClient": true,
      "redirectUris": ["http://localhost:5173/*"]
    }
  ],
  "users": [
    {
      "username": "admin",
      "enabled": true,
      "credentials": [{"type":"password","value":"Admin1234"}],
      "realmRoles": ["admin"]
    }
  ],
  "roles": {"realm": ["admin","user"]}
}
EOF

# levantar keycloak usando la imagen oficial (adaptar version si es necesario)
docker run --name keycloak-pymerp -p 8082:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:21.1.1 start-dev --import-realm
```

2) Acceder a Keycloak Admin Console
- URL: `http://localhost:8082/`
- Usuario: `admin` / `admin` (creado por variable de entorno)
- Ver el realm `pymerp` y el client `pymerp-backend`.

3) Obtener token (password grant, si el client permite)

```bash
# ejemplo con curl (ajusta host/realm si cambia)
curl -X POST "http://localhost:8082/realms/pymerp/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=pymerp-backend&username=admin&password=Admin1234"
```

Respuesta: JSON con `access_token`.

4) Llamar a la API protegida

```bash
curl -X GET "http://localhost:8081/api/v1/products" \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H "X-Company-Id: 00000000-0000-0000-0000-000000000001"
```

Notas
- En este quickstart el client `pymerp-backend` se dejó como `publicClient=true` para simplificar pruebas; en producción debes usar `confidential` y client secret o Authorization Code flow.
- Si usas `publicClient`, Keycloak no exige client secret para el token endpoint con grant_type=password.
- Ajusta la versión de Keycloak según compatibilidad.

Siguientes pasos
- Importar un realm más completo con mappers para poner roles en `realm_access.roles` o en claims personalizados.
- Automatizar la creación del realm con `docker-compose` y un archivo `realm-export.json` incluido en el repo.
