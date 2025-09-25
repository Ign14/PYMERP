import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/app_config_provider.dart';
import '../data/auth_repository.dart';
import '../domain/auth_session.dart';

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  final config = ref.watch(appConfigProvider);
  return AuthRepository(config: config);
});

final authHeadersProvider = Provider<AuthHeadersProvider>((ref) {
  return AuthHeadersProvider(() async {
    final repo = ref.read(authRepositoryProvider);
    final config = ref.read(appConfigProvider);
    final token = await repo.getValidAccessToken();
    final headers = <String, String>{'X-Company-Id': config.companyId};
    if (token != null && token.isNotEmpty) {
      headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  });
});

final authNotifierProvider = AsyncNotifierProvider<AuthNotifier, AuthSession?>(() {
  return AuthNotifier();
});

class AuthHeadersProvider {
  const AuthHeadersProvider(this._getter);

  final Future<Map<String, String>> Function() _getter;

  Future<Map<String, String>> call() => _getter();
}

class AuthNotifier extends AsyncNotifier<AuthSession?> {
  @override
  Future<AuthSession?> build() async {
    final repo = ref.watch(authRepositoryProvider);
    return repo.ensureSession();
  }

  Future<void> login({required String email, required String password}) async {
    final repo = ref.watch(authRepositoryProvider);
    state = const AsyncLoading();
    state = await AsyncValue.guard(() async {
      final session = await repo.login(email: email, password: password);
      return session;
    });
  }

  Future<void> logout() async {
    final repo = ref.watch(authRepositoryProvider);
    await repo.clear();
    state = const AsyncData(null);
  }

  Future<AuthSession?> refreshIfNeeded() async {
    final repo = ref.watch(authRepositoryProvider);
    final refreshed = await repo.ensureSession();
    state = AsyncData(refreshed);
    return refreshed;
  }
}
