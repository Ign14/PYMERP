class Env {
  // FIX: Alinear URL base por defecto con el backend (usa /api/v1 para que endpoints en Dart como
  // '/billing/documents/...' resuelvan a 'http://localhost:8080/api/v1/billing/...')
  static const String baseUrl =
      String.fromEnvironment('BASE_URL', defaultValue: 'http://localhost:8080/api/v1');
  static const String companyId = String.fromEnvironment('COMPANY_ID', defaultValue: 'dev-company');
  static const bool useHttps = bool.fromEnvironment('USE_HTTPS', defaultValue: false);
}
