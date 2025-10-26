import 'dart:async';
import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/billing_repository.dart';
import '../domain/billing_models.dart';
import 'billing_providers.dart';

final documentoEmitidoProvider = AutoDisposeAsyncNotifierProviderFamily<
    DocumentoEmitidoNotifier, DocumentWatcherState, String>(
  DocumentoEmitidoNotifier.new,
);

class DocumentWatcherState {
  const DocumentWatcherState({
    required this.document,
    required this.history,
    required this.viewerReloadToken,
    required this.officialRefreshTick,
    this.error,
  });

  final BillingDocument document;
  final Map<String, String> history;
  final int viewerReloadToken;
  final int officialRefreshTick;
  final Object? error;

  DocumentWatcherState copyWith({
    BillingDocument? document,
    Map<String, String>? history,
    int? viewerReloadToken,
    int? officialRefreshTick,
    Object? error = _sentinel,
  }) {
    return DocumentWatcherState(
      document: document ?? this.document,
      history: history ?? this.history,
      viewerReloadToken: viewerReloadToken ?? this.viewerReloadToken,
      officialRefreshTick: officialRefreshTick ?? this.officialRefreshTick,
      error: error == _sentinel ? this.error : error,
    );
  }

  bool get showContingencyBadge => document.isContingency;
  DocumentStatus get status => document.status;
  DocumentVersion get version => document.version;
  String get pdfUrl => document.pdfUrl;
}

class DocumentoEmitidoNotifier
    extends AutoDisposeFamilyAsyncNotifier<DocumentWatcherState, String> {
  late final BillingDataSource _repository;
  late String _documentId;
  StreamSubscription<BillingDocument>? _subscription;
  Completer<void>? _refreshing;

  @override
  Future<DocumentWatcherState> build(String documentId) async {
    _repository = ref.watch(billingRepositoryProvider);
    _documentId = documentId;
    ref.onDispose(_dispose);

    final initial = await _repository.getDocument(documentId);
    _subscription = _repository.watchDocument(documentId).listen(
          _handleIncomingDocument,
          onError: _handleError,
        );
    return DocumentWatcherState(
      document: initial,
      history: _composeHistory(initial, const <String, String>{}),
      viewerReloadToken: 0,
      officialRefreshTick: _isOfficial(initial) ? 1 : 0,
    );
  }

  Future<void> refresh() async {
    if (_refreshing != null) return _refreshing!.future;
    _refreshing = Completer<void>();
    try {
      final updated = await _repository.getDocument(_documentId);
      _handleIncomingDocument(updated);
      _refreshing?.complete();
    } catch (error, stackTrace) {
      _handleError(error, stackTrace);
      _refreshing?.completeError(error, stackTrace);
    } finally {
      _refreshing = null;
    }
  }

  Future<Uint8List?> fetchPdfBytes() async {
    final document = state.valueOrNull?.document;
    if (document == null || document.pdfUrl.isEmpty) return null;
    return _repository.downloadPdf(document.pdfUrl);
  }

  void _handleIncomingDocument(BillingDocument document) {
    final current = state.valueOrNull;
    if (current == null) {
      state = AsyncData(
        DocumentWatcherState(
          document: document,
          history: _composeHistory(document, const <String, String>{}),
          viewerReloadToken: 0,
          officialRefreshTick: _isOfficial(document) ? 1 : 0,
        ),
      );
      return;
    }
    var viewerToken = current.viewerReloadToken;
    var officialTick = current.officialRefreshTick;

    final wasOfficial = _isOfficial(current.document);
    final isOfficial = _isOfficial(document);

    if (!wasOfficial && isOfficial) {
      viewerToken += 1;
      officialTick += 1;
    } else if (current.document.pdfUrl != document.pdfUrl &&
        document.pdfUrl.isNotEmpty) {
      viewerToken += 1;
    }

    final mergedHistory = _composeHistory(document, current.history);

    state = AsyncData(
      current.copyWith(
        document: document,
        history: mergedHistory,
        viewerReloadToken: viewerToken,
        officialRefreshTick: officialTick,
        error: null,
      ),
    );
  }

  void _handleError(Object error, StackTrace stackTrace) {
    final current = state.valueOrNull;
    if (current == null) {
      state = AsyncError(error, stackTrace);
      return;
    }
    state = AsyncData(current.copyWith(error: error));
  }

  Map<String, String> _composeHistory(
      BillingDocument document, Map<String, String> previous) {
    final history = <String, String>{};
    history.addAll(previous);
    history.addAll(document.history);
    if (document.number != null && document.number!.isNotEmpty) {
      history[document.provisionalNumber] = document.number!;
    }
    return history;
  }

  bool _isOfficial(BillingDocument document) =>
      document.status == DocumentStatus.accepted &&
      document.version == DocumentVersion.official;

  Future<void> _dispose() async {
    await _subscription?.cancel();
    _subscription = null;
  }
}

const _sentinel = Object();
