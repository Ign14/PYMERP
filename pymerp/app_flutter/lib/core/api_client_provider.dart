import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'api_client.dart';
import 'app_config_provider.dart';
import '../modules/auth/application/auth_notifier.dart';

final apiClientProvider = Provider<ApiClient>((ref) {
  final config = ref.watch(appConfigProvider);
  final headersProvider = ref.watch(authHeadersProvider);
  final authRepository = ref.watch(authRepositoryProvider);
  return ApiClient(
    baseUrl: config.baseUrl,
    headersProvider: headersProvider,
    authRepository: authRepository,
  );
});
