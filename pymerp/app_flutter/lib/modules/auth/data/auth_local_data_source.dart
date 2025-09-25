import '../../../core/secure_storage.dart';
import '../domain/auth_session.dart';

class AuthLocalDataSource {
  AuthLocalDataSource({SecureStorage? storage}) : _storage = storage ?? SecureStorage();

  final SecureStorage _storage;
  static const _sessionKey = 'auth_session';

  Future<AuthSession?> read() async {
    final raw = await _storage.read(_sessionKey);
    return AuthSession.fromStorageString(raw);
  }

  Future<void> write(AuthSession session) async {
    await _storage.write(_sessionKey, session.toStorageString());
  }

  Future<void> clear() async {
    await _storage.delete(_sessionKey);
  }
}
