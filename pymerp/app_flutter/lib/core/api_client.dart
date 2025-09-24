import 'package:dio/dio.dart';
import 'config.dart';
import 'storage.dart';

class ApiClient {
  final Dio dio;
  ApiClient._internal(this.dio);

  factory ApiClient(AppConfig cfg) {
    final dio = Dio(BaseOptions(baseUrl: cfg.baseUrl));

    dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await TokenStorage.read();
        if (token != null && token.isNotEmpty) {
          options.headers['Authorization'] = 'Bearer ' + token;
        }
        options.headers['X-Company-Id'] = cfg.companyId;
        handler.next(options);
      },
      onError: (e, handler) {
        handler.next(e);
      },
    ));

    return ApiClient._internal(dio);
  }
}
