import 'package:dio/dio.dart';

import '../modules/auth/application/auth_notifier.dart';
import '../modules/auth/data/auth_repository.dart';

class ApiClient {
  ApiClient({
    required String baseUrl,
    required AuthHeadersProvider headersProvider,
    required AuthRepository authRepository,
    Dio? dioClient,
  })  : _headersProvider = headersProvider,
        _authRepository = authRepository,
        dio = dioClient ?? Dio(BaseOptions(baseUrl: baseUrl)) {
    dio.interceptors.add(QueuedInterceptorsWrapper(
      onRequest: (options, handler) async {
        final headers = await _headersProvider();
        options.headers.addAll(headers);
        handler.next(options);
      },
      onError: (error, handler) async {
        if (await _shouldRefresh(error)) {
          try {
            await _authRepository.refreshToken();
            final requestOptions = error.requestOptions;
            requestOptions.extra['__retried'] = true;
            requestOptions.headers.remove('Authorization');
            final response = await dio.fetch(requestOptions);
            handler.resolve(response);
            return;
          } catch (_) {
            await _authRepository.clear();
          }
        }
        handler.next(error);
      },
    ));
  }

  final Dio dio;
  final AuthHeadersProvider _headersProvider;
  final AuthRepository _authRepository;

  Future<bool> _shouldRefresh(DioException error) async {
    if (error.response?.statusCode != 401) return false;
    final retried = error.requestOptions.extra['__retried'] == true;
    if (retried) return false;
    return await _authRepository.hasValidRefreshToken();
  }
}
