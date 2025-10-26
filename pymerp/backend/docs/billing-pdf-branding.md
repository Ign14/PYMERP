## Billing PDF Branding

Los PDF locales se generan con plantillas HTML ubicadas en `src/main/resources/templates/billing`. Puede
personalizarlos sin tocar el código aprovechando las propiedades expuestas en `application.yml`:

```yaml
billing:
  offline:
    legend: "Documento emitido en contingencia..."
  pdf:
    documents-base-url: https://app.midominio.cl/api/v1
    branding:
      logo-path: "file:/opt/pymerp/branding/logo.png"
      primary-color: "#1f2937"
      accent-color: "#2563eb"
      text-color: "#111827"
      table-header-color: "#e5e7eb"
```

Notas:

- `logo-path` acepta rutas `file:` o `classpath:`. Si se omite el logo se renderiza un marcador de posición con las
  iniciales de la empresa.
- Los colores solo admiten valores hexadecimales; conviene mantener suficiente contraste para tablas y totales.
- Cambiar la leyenda de contingencia (`billing.offline.legend`) actualiza automáticamente el banner que aparece en los
  PDF generados sin conexión.

Tras actualizar las propiedades reinicie el servicio para que el renderizador cargue la nueva configuración. No es
necesario recompilar mientras las plantillas HTML no se modifiquen.
