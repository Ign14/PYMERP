import 'env.dart';

class AppConfig {
  final String baseUrl;
  final String companyId;
  final bool useHttps;
  const AppConfig({required this.baseUrl, required this.companyId, required this.useHttps});

  factory AppConfig.fromEnv() => AppConfig(
        baseUrl: Env.baseUrl,
        companyId: Env.companyId,
        useHttps: Env.useHttps,
      );
}
