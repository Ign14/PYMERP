import 'dart:io';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:file_saver/file_saver.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:pymerp_app/modules/billing/application/billing_providers.dart';
import 'package:pymerp_app/modules/billing/application/documento_emitido_notifier.dart';
import 'package:pymerp_app/modules/billing/data/billing_repository.dart';
import 'package:pymerp_app/modules/billing/domain/billing_models.dart';
import 'package:pymerp_app/modules/billing/presentation/documento_emitido_view.dart';

class _FakeBillingRepository implements BillingDataSource {
  _FakeBillingRepository(this._documents, {Uint8List? pdfBytes})
      : _pdfBytes = pdfBytes ?? Uint8List(0);

  final Map<String, BillingDocument> _documents;
  final Uint8List _pdfBytes;
  int downloadPdfCalls = 0;
  String? lastPdfUrl;

  @override
  Future<BillingDocument> getDocument(String documentId) async {
    final document = _documents[documentId];
    if (document == null) {
      throw StateError('document not found');
    }
    return document;
  }

  @override
  Stream<BillingDocument> watchDocument(String documentId) {
    return const Stream<BillingDocument>.empty();
  }

  @override
  Future<String> submitSale(SaleRequest request) async =>
      'doc-${request.documentType.wireValue}';

  @override
  Future<Uint8List> downloadPdf(String url) async {
    downloadPdfCalls += 1;
    lastPdfUrl = url;
    return _pdfBytes;
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  DocumentWatcherState stateFor({
    required DocumentStatus status,
    required DocumentVersion version,
    Map<String, String> history = const <String, String>{},
    String? number,
    String pdfUrl = '',
  }) {
    final document = BillingDocument(
      id: 'doc-1',
      status: status,
      version: version,
      pdfUrl: pdfUrl,
      provisionalNumber: 'TMP-001',
      history: history,
      number: number,
    );
    final mergedHistory = <String, String>{}..addAll(history);
    if (number != null) {
      mergedHistory['TMP-001'] = number;
    }
    return DocumentWatcherState(
      document: document,
      history: mergedHistory,
      viewerReloadToken: 0,
      officialRefreshTick: status == DocumentStatus.accepted &&
              version == DocumentVersion.official
          ? 1
          : 0,
    );
  }

  Widget buildSubject(DocumentWatcherState state,
      {_FakeBillingRepository? repository}) {
    final repo = repository ?? _FakeBillingRepository(<String, BillingDocument>{
      state.document.id: state.document,
    });

    return ProviderScope(
      overrides: <Override>[
        billingRepositoryProvider.overrideWithValue(repo),
      ],
      child: const MaterialApp(
        home: DocumentoEmitidoView(documentId: 'doc-1'),
      ),
    );
  }

  tearDown(() {
    debugFileSaver = null;
    debugPrintLayout = null;
  });

  testWidgets(
      'muestra badge de contingencia cuando el estado es OFFLINE_PENDING',
      (tester) async {
    final state = stateFor(
      status: DocumentStatus.offlinePending,
      version: DocumentVersion.local,
    );

    await tester.pumpWidget(buildSubject(state));
    await tester.pumpAndSettle();

    expect(find.text('Contingencia'), findsOneWidget);
    expect(find.textContaining('Se enviara automaticamente'), findsOneWidget);
  });

  testWidgets('muestra estado SENT sin contingencia', (tester) async {
    final state = stateFor(
      status: DocumentStatus.sent,
      version: DocumentVersion.official,
    );

    await tester.pumpWidget(buildSubject(state));
    await tester.pumpAndSettle();

    expect(find.text('SENT'), findsOneWidget);
    expect(find.text('Contingencia'), findsNothing);
  });

  testWidgets('muestra estado ACCEPTED con version oficial y numeracion',
      (tester) async {
    final history = <String, String>{'TMP-001': 'F001-123'};
    final state = stateFor(
      status: DocumentStatus.accepted,
      version: DocumentVersion.official,
      history: history,
      number: 'F001-123',
    );

    await tester.pumpWidget(buildSubject(state));
    await tester.pumpAndSettle();

    expect(find.text('ACCEPTED'), findsOneWidget);
    expect(find.textContaining('Version OFFICIAL'), findsOneWidget);
    expect(find.text('Contingencia'), findsNothing);
    expect(find.text('F001-123'), findsWidgets);
  });

  testWidgets('muestra estado REJECTED y acciones disponibles', (tester) async {
    final state = stateFor(
      status: DocumentStatus.rejected,
      version: DocumentVersion.local,
    );

    await tester.pumpWidget(buildSubject(state));
    await tester.pump();

    expect(find.text('REJECTED'), findsOneWidget);
    expect(find.text('Guardar PDF'), findsOneWidget);
    expect(find.text('Imprimir'), findsOneWidget);
  });

  testWidgets('guardar PDF descarga bytes y utiliza FileSaver', (tester) async {
    final state = stateFor(
      status: DocumentStatus.accepted,
      version: DocumentVersion.official,
      number: 'F001-555',
      pdfUrl: 'https://example.com/invoice.pdf',
    );
    final repository = _FakeBillingRepository(
      <String, BillingDocument>{state.document.id: state.document},
      pdfBytes: Uint8List.fromList(<int>[1, 2, 3]),
    );
    final saver = _RecordingFileSaver();
    debugFileSaver = saver;

    final guardarButton = find.text('Guardar PDF');
    await tester.pumpWidget(buildSubject(state, repository: repository));
    for (var i = 0; i < 10 && guardarButton.evaluate().isEmpty; i++) {
      await tester.pump(const Duration(milliseconds: 50));
    }
    expect(guardarButton, findsOneWidget);

    final initialCalls = repository.downloadPdfCalls;
    await tester.tap(guardarButton);
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));

    expect(repository.downloadPdfCalls, initialCalls + 1);
    expect(repository.lastPdfUrl, 'https://example.com/invoice.pdf');
    expect(saver.saveFileCalls, 1);
    expect(saver.lastFilename, contains('F001-555'));
    expect(saver.lastBytes, isNotNull);
    expect(saver.lastBytes!, orderedEquals(<int>[1, 2, 3]));
  });

  testWidgets('imprimir descarga bytes y envia a Printing', (tester) async {
    final state = stateFor(
      status: DocumentStatus.accepted,
      version: DocumentVersion.official,
      number: 'F001-777',
      pdfUrl: 'https://example.com/invoice.pdf',
    );
    final repository = _FakeBillingRepository(
      <String, BillingDocument>{state.document.id: state.document},
      pdfBytes: Uint8List.fromList(<int>[7, 8, 9]),
    );
    final printedBytes = <Uint8List>[];
    debugPrintLayout = (Uint8List bytes) async {
      printedBytes.add(bytes);
      return true;
    };

    final imprimirButton = find.text('Imprimir');
    await tester.pumpWidget(buildSubject(state, repository: repository));
    for (var i = 0; i < 10 && imprimirButton.evaluate().isEmpty; i++) {
      await tester.pump(const Duration(milliseconds: 50));
    }
    expect(imprimirButton, findsOneWidget);

    final baseCalls = repository.downloadPdfCalls;
    await tester.tap(imprimirButton);
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 100));

    expect(repository.downloadPdfCalls, baseCalls + 1);
    expect(printedBytes, hasLength(1));
    expect(printedBytes.single, orderedEquals(<int>[7, 8, 9]));
  });
}

class _RecordingFileSaver extends FileSaver {
  int saveFileCalls = 0;
  Uint8List? lastBytes;
  String? lastFilename;

  @override
  Future<String> saveFile({
    required String name,
    Uint8List? bytes,
    File? file,
    String? filePath,
    LinkDetails? link,
    String ext = '',
    MimeType mimeType = MimeType.other,
    String? customMimeType,
    Dio? dioClient,
    Uint8List Function(dynamic data)? transformDioResponse,
  }) async {
    saveFileCalls += 1;
    lastFilename = '$name$ext';
    lastBytes = bytes;
    return 'memory/$name$ext';
  }
}
