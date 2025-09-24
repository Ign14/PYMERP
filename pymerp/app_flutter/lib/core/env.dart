class Env {
  static const String baseUrl = String.fromEnvironment('BASE_URL', defaultValue: 'http://localhost:8080');
  static const String companyId = String.fromEnvironment('COMPANY_ID', defaultValue: 'dev-company');
  static const bool useHttps = bool.fromEnvironment('USE_HTTPS', defaultValue: false);
}
