import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/storage.dart';

final authTokenProvider = StateProvider<String?>((ref) => null);

class AuthController {
  final StateController<String?> _tokenState;
  AuthController(this._tokenState);

  Future<void> loginWithToken(String token) async {
    await TokenStorage.save(token);
    _tokenState.state = token;
  }

  Future<void> logout() async {
    await TokenStorage.clear();
    _tokenState.state = null;
  }
}
