import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:uuid/uuid.dart';

import '../../../core/api_client.dart';
import '../domain/billing_models.dart';

typedef WebSocketConnector = WebSocketChannel Function(Uri uri);

const Uuid _uuid = Uuid();

abstract class BillingDataSource {
  Future<String> submitSale(SaleRequest request);
  Future<BillingDocument> getDocument(String documentId);
  Stream<BillingDocument> watchDocument(String documentId);
  Future<Uint8List> downloadPdf(String url);
}

class BillingRepository implements BillingDataSource {
  BillingRepository(
    this._client, {
    WebSocketConnector? webSocketConnector,
    Duration? pollingInterval,
  })  : _webSocketConnector = webSocketConnector ?? WebSocketChannel.connect,
        _pollingInterval = pollingInterval ?? const Duration(seconds: 6);

  final ApiClient _client;
  final WebSocketConnector _webSocketConnector;
  final Duration _pollingInterval;

  Dio get _dio => _client.dio;

  @override
  Future<String> submitSale(SaleRequest request) async {
    final endpoint = request.documentType.isFiscal
        ? '/billing/invoices'
        : '/billing/non-fiscal';
    final key = _idempotencyKeyFor(request);
    final response = await _postWithRetry<String>(
      () async {
        final res = await _dio.post<Map<String, dynamic>>(
          endpoint,
          data: request.toJson(),
          options: Options(headers: <String, dynamic>{
            'Idempotency-Key': key, // FIX: required header for backend idempotency.
          }),
        );
        final data = res.data ?? const <String, dynamic>{};
        final id = data['id'] ?? data['documentId'] ?? data['document_id'];
        if (id == null) {
          throw StateError(
              'La respuesta no incluyo el identificador del documento.');
        }
        return id.toString();
      },
    );
    return response;
  }

  @override
  Future<BillingDocument> getDocument(String documentId) async {
    final response = await _getWithRetry<Map<String, dynamic>>(
      () async {
        final res = await _dio
            .get<Map<String, dynamic>>('/billing/documents/$documentId');
        if (res.data == null) {
          throw StateError('La respuesta del documento esta vacia.');
        }
        return res.data!;
      },
    );
    return BillingDocument.fromJson(response);
  }

  @override
  Stream<BillingDocument> watchDocument(String documentId) {
    final controller = StreamController<BillingDocument>();
    // FIX: Reemplazar uso complejo de WebSocket por polling HTTP simple.
    // Esto simplifica el flujo y evita dependencias de WebSocket del servidor.
    Timer? poller;

    Future<void> emitOnce() async {
      try {
        final document = await _pollOnce(documentId);
        if (!controller.isClosed) {
          controller.add(document);
        }
      } catch (error, stackTrace) {
        if (!controller.isClosed) {
          controller.addError(error, stackTrace);
        }
      }
    }

    void startPolling() {
      // Emit initial immediately
      unawaited(emitOnce());
      poller = Timer.periodic(_pollingInterval, (_) {
        unawaited(emitOnce());
      });
    }

    controller.onListen = startPolling;
    controller.onResume = startPolling;
    controller.onCancel = () async {
      poller?.cancel();
      poller = null;
      if (!controller.isClosed) {
        await controller.close();
      }
    };

    return controller.stream;
  }

  Future<BillingDocument> _pollOnce(String documentId) async {
    return getDocument(documentId);
  }

  @override
  Future<Uint8List> downloadPdf(String url) async {
    final target = _resolveUrl(url);
    final response = await _getWithRetry<Response<Uint8List>>(
      () async => _dio.get<Uint8List>(
        target.toString(),
        options:
            Options(responseType: ResponseType.bytes, followRedirects: true),
      ),
    );
    return response.data ?? Uint8List.fromList(const <int>[]);
  }

  String _idempotencyKeyFor(SaleRequest request) {
    final existing = request.idempotencyKey;
    if (existing != null && existing.isNotEmpty) {
      return existing;
    }
    final saleId = request.sale.id;
    final key = saleId.isNotEmpty
        ? 'sale-$saleId-${request.documentType.wireValue}'
        : _uuid.v4();
    request.idempotencyKey = key; // FIX: persist key for retries.
    return key;
  }

  Future<T> _postWithRetry<T>(Future<T> Function() fn,
      {int retries = 3}) async {
    var attempt = 0;
    DioException? lastError;
    while (attempt < retries) {
      try {
        return await fn();
      } on DioException catch (error) {
        lastError = error;
        attempt += 1;
        if (attempt >= retries || !_shouldRetry(error)) {
          rethrow;
        }
        await Future<void>.delayed(Duration(milliseconds: 400 * attempt));
      }
    }
    throw lastError ?? StateError('No fue posible completar la solicitud');
  }

  Future<T> _getWithRetry<T>(Future<T> Function() fn, {int retries = 3}) async {
    var attempt = 0;
    DioException? lastError;
    while (attempt < retries) {
      try {
        return await fn();
      } on DioException catch (error) {
        lastError = error;
        attempt += 1;
        if (attempt >= retries || !_shouldRetry(error)) {
          rethrow;
        }
        await Future<void>.delayed(Duration(milliseconds: 400 * attempt));
      }
    }
    throw lastError ?? StateError('No fue posible completar la solicitud');
  }

  bool _shouldRetry(DioException error) {
    if (error.type == DioExceptionType.cancel) return false;
    if (error.response == null) return true;
    final statusCode = error.response!.statusCode ?? 0;
    return statusCode >= 500 || statusCode == 408;
  }

  Uri _resolveUrl(String url) {
    final uri = Uri.parse(url);
    if (uri.hasScheme) return uri;
    final base = Uri.parse(_dio.options.baseUrl);
    return base.resolveUri(uri);
  }

  Uri _buildWebSocketUri(String documentId) {
    final base = Uri.parse(_dio.options.baseUrl);
    final segments = <String>[
      ...base.pathSegments.where((segment) => segment.isNotEmpty),
      'billing',
      'documents',
      documentId,
      'ws',
    ];
    final scheme = base.scheme == 'https' ? 'wss' : 'ws';
    return base.replace(
      scheme: scheme,
      userInfo: base.userInfo,
      host: base.host,
      port: base.hasPort ? base.port : null,
      pathSegments: segments,
      queryParameters: <String, dynamic>{},
    );
  }
}
