import 'package:dio/dio.dart';

import '../../../core/config.dart';
import '../domain/auth_session.dart';
import 'auth_local_data_source.dart';

class AuthRepository {
  AuthRepository({required AppConfig config, Dio? http, AuthLocalDataSource? local})
      : _dio = http ?? Dio(BaseOptions(baseUrl: config.baseUrl)),
        _local = local ?? AuthLocalDataSource(),
        _config = config;

  final Dio _dio;
  final AuthLocalDataSource _local;
  final AppConfig _config;

  AuthSession? _cache;
  bool _refreshing = false;

  Future<AuthSession?> loadSession() async {
    if (_cache != null) return _cache;
    _cache = await _local.read();
    return _cache;
  }

  Future<AuthSession?> ensureSession() async {
    final session = await loadSession();
    if (session == null) return null;
    if (!session.isAccessTokenExpired) {
      return session;
    }
    if (session.isRefreshTokenExpired) {
      await clear();
      return null;
    }
    return await refreshToken();
  }

  Future<String?> getValidAccessToken() async {
    final session = await ensureSession();
    return session?.accessToken;
  }

  Future<bool> hasValidRefreshToken() async {
    final session = await loadSession();
    if (session == null) return false;
    return !session.isRefreshTokenExpired;
  }

  Future<AuthSession> login({required String email, required String password}) async {
    final response = await _dio.post<Map<String, dynamic>>('/api/v1/auth/login', data: {
      'email': email,
      'password': password,
      'companyId': _config.companyId,
    });
    final session = _mapSession(response.data!);
    await _persist(session);
    return session;
  }

  Future<AuthSession> refreshToken() async {
    if (_refreshing) {
      // If another refresh is already running wait for it to complete.
      while (_refreshing) {
        await Future<void>.delayed(const Duration(milliseconds: 50));
      }
      final session = await loadSession();
      if (session == null) {
        throw StateError('Session not available after refresh');
      }
      if (session.isAccessTokenExpired && !session.isRefreshTokenExpired) {
        // No new token yet, retry real refresh.
      } else {
        return session;
      }
    }
    final session = await loadSession();
    if (session == null) {
      throw StateError('Cannot refresh without a session');
    }
    if (session.isRefreshTokenExpired) {
      await clear();
      throw StateError('Refresh token expired');
    }
    try {
      _refreshing = true;
      final response = await _dio.post<Map<String, dynamic>>('/api/v1/auth/refresh', data: {
        'refreshToken': session.refreshToken,
        'companyId': _config.companyId,
      });
      final refreshed = _mapSession(response.data!);
      await _persist(refreshed);
      return refreshed;
    } finally {
      _refreshing = false;
    }
  }

  Future<void> clear() async {
    _cache = null;
    await _local.clear();
  }

  Future<void> _persist(AuthSession session) async {
    _cache = session;
    await _local.write(session);
  }

  AuthSession _mapSession(Map<String, dynamic> json) {
    final now = DateTime.now();
    final expiresIn = json['expiresIn'] as num;
    final refreshExpiresIn = json['refreshExpiresIn'] as num;
    return AuthSession(
      accessToken: json['token'] as String,
      accessTokenExpiresAt: now.add(Duration(seconds: expiresIn.toInt())),
      refreshToken: json['refreshToken'] as String,
      refreshTokenExpiresAt: now.add(Duration(seconds: refreshExpiresIn.toInt())),
      companyId: (json['companyId'] ?? _config.companyId).toString(),
      email: json['email'] as String? ?? '',
      name: json['name'] as String? ?? '',
    );
  }
}
