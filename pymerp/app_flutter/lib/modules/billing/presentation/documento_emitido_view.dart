import 'dart:async';
import 'package:file_saver/file_saver.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_pdfview/flutter_pdfview.dart';
import 'package:path_provider/path_provider.dart';
import 'package:printing/printing.dart';
import 'package:universal_html/html.dart' as html;
import 'package:universal_io/io.dart' as io;

import '../application/documento_emitido_notifier.dart';
import '../domain/billing_models.dart';
import 'pdf_viewer_registry.dart';

final Set<String> _registeredViewTypes = <String>{};

@visibleForTesting
FileSaver? debugFileSaver;

FileSaver _fileSaver() => debugFileSaver ?? FileSaver.instance;

@visibleForTesting
Future<bool> Function(Uint8List bytes)? debugPrintLayout;

Future<bool> _layoutPdf(Uint8List bytes) {
  if (debugPrintLayout != null) {
    return debugPrintLayout!(bytes);
  }
  return Printing.layoutPdf(onLayout: (_) async => bytes);
}

class DocumentoEmitidoView extends ConsumerStatefulWidget {
  const DocumentoEmitidoView({required this.documentId, super.key});

  final String documentId;

  @override
  ConsumerState<DocumentoEmitidoView> createState() =>
      _DocumentoEmitidoViewState();
}

class _DocumentoEmitidoViewState extends ConsumerState<DocumentoEmitidoView> {
  @override
  void initState() {
    super.initState();
    unawaited(ref.read(documentoEmitidoProvider(widget.documentId).future));
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<AsyncValue<DocumentWatcherState>>(
      documentoEmitidoProvider(widget.documentId),
      (previous, next) {
        final previousData = previous?.valueOrNull;
        final nextData = next.valueOrNull;
        if (previousData == null || nextData == null) return;
        if (nextData.officialRefreshTick > previousData.officialRefreshTick) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Documento oficial disponible')),
          );
        }
      },
    );

    final asyncState = ref.watch(documentoEmitidoProvider(widget.documentId));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Documento emitido'),
        actions: <Widget>[
          IconButton(
            tooltip: 'Refrescar',
            onPressed: () => ref
                .read(documentoEmitidoProvider(widget.documentId).notifier)
                .refresh(),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: asyncState.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => _DocumentoErrorView(
          error: error,
          onRetry: () => ref
              .read(documentoEmitidoProvider(widget.documentId).notifier)
              .refresh(),
        ),
        data: (data) => _DocumentoEmitidoContent(
          documentId: widget.documentId,
          state: data,
        ),
      ),
    );
  }
}

class _DocumentoEmitidoContent extends ConsumerStatefulWidget {
  const _DocumentoEmitidoContent(
      {required this.documentId, required this.state});

  final String documentId;
  final DocumentWatcherState state;

  @override
  ConsumerState<_DocumentoEmitidoContent> createState() =>
      _DocumentoEmitidoContentState();
}

class _DocumentoEmitidoContentState
    extends ConsumerState<_DocumentoEmitidoContent> {
  bool _saving = false;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final historyEntries = widget.state.history.entries.toList()
      ..sort((a, b) => a.key.compareTo(b.key));

    Future<void> onSave() async {
      if (widget.state.pdfUrl.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('El PDF aun no esta disponible.')),
        );
        return;
      }
      setState(() => _saving = true);
      try {
        final bytes = await ref
            .read(documentoEmitidoProvider(widget.documentId).notifier)
            .fetchPdfBytes();
        if (bytes == null || bytes.isEmpty) {
          throw StateError('No se pudo descargar el PDF.');
        }
        final filename = _buildFilename(widget.state);
        if (kIsWeb) {
          _downloadWeb(bytes, filename);
        } else {
          await _fileSaver().saveFile(
            name: filename,
            bytes: bytes,
            mimeType: MimeType.pdf,
            ext: 'pdf',
          );
        }
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('PDF guardado como $filename')),
          );
        }
      } catch (error) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error al guardar: $error')),
          );
        }
      } finally {
        if (mounted) {
          setState(() => _saving = false);
        }
      }
    }

    Future<void> onPrint() async {
      if (widget.state.pdfUrl.isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('El PDF aun no esta disponible.')),
        );
        return;
      }
      try {
        final bytes = await ref
            .read(documentoEmitidoProvider(widget.documentId).notifier)
            .fetchPdfBytes();
        if (bytes == null || bytes.isEmpty) {
          throw StateError('No se pudo descargar el PDF.');
        }
        await _layoutPdf(bytes);
      } catch (error) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error al imprimir: $error')),
          );
        }
      }
    }

    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: <Widget>[
          Row(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: <Widget>[
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      'Documento ${widget.state.document.number ?? widget.state.document.provisionalNumber}',
                      style: theme.textTheme.titleLarge,
                    ),
                    const SizedBox(height: 8),
                    Wrap(
                      spacing: 12,
                      runSpacing: 8,
                      children: <Widget>[
                        Chip(
                          backgroundColor:
                              _statusColor(theme, widget.state.status),
                          label: Text(widget.state.status.wireValue),
                        ),
                        Chip(
                          avatar: const Icon(Icons.verified, size: 18),
                          label:
                              Text('Version ${widget.state.version.wireValue}'),
                        ),
                        if (widget.state.showContingencyBadge)
                          Badge(
                            backgroundColor: theme.colorScheme.errorContainer,
                            label: const Text('Contingencia'),
                          ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 16),
              Wrap(
                spacing: 12,
                children: <Widget>[
                  OutlinedButton.icon(
                    onPressed: _saving ? null : onSave,
                    icon: _saving
                        ? const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.download),
                    label: const Text('Guardar PDF'),
                  ),
                  FilledButton.icon(
                    onPressed: onPrint,
                    icon: const Icon(Icons.print),
                    label: const Text('Imprimir'),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 24),
          if (widget.state.showContingencyBadge)
            Card(
              color: theme.colorScheme.surfaceContainerHighest,
              child: const ListTile(
                leading: Icon(Icons.wifi_off),
                title: Text(
                    'Se enviara automaticamente cuando vuelva la conexion.'),
              ),
            ),
          const SizedBox(height: 16),
          Expanded(
            child: _PdfViewerSection(
              documentId: widget.documentId,
              state: widget.state,
            ),
          ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(
                    'Historial de numeracion',
                    style: theme.textTheme.titleMedium,
                  ),
                  const SizedBox(height: 12),
                  if (historyEntries.isEmpty)
                    const Text('Aun no hay numeracion oficial.')
                  else
                    ...historyEntries.map(
                      (entry) => ListTile(
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                        title: Text('Prov. ${entry.key}'),
                        trailing: Text(entry.value),
                      ),
                    ),
                ],
              ),
            ),
          ),
          if (widget.state.error != null) ...<Widget>[
            const SizedBox(height: 12),
            Text(
              'Ultimo error: ${widget.state.error}',
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.error),
            ),
          ],
        ],
      ),
    );
  }

  static Color _statusColor(ThemeData theme, DocumentStatus status) {
    switch (status) {
      case DocumentStatus.accepted:
        return theme.colorScheme.secondaryContainer;
      case DocumentStatus.sent:
        return theme.colorScheme.tertiaryContainer;
      case DocumentStatus.offlinePending:
        return theme.colorScheme.errorContainer;
      case DocumentStatus.rejected:
        return theme.colorScheme.errorContainer;
      case DocumentStatus.cancelled:
        return theme.colorScheme.outlineVariant;
      case DocumentStatus.unknown:
        return theme.colorScheme.surfaceContainerHighest;
    }
  }
}

class _PdfViewerSection extends ConsumerStatefulWidget {
  const _PdfViewerSection({required this.documentId, required this.state});

  final String documentId;
  final DocumentWatcherState state;

  @override
  ConsumerState<_PdfViewerSection> createState() => _PdfViewerSectionState();
}

class _PdfViewerSectionState extends ConsumerState<_PdfViewerSection> {
  String? _filePath;
  Object? _error;
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    if (!_shouldShowPlaceholder()) {
      unawaited(_loadPdf());
    }
  }

  @override
  void didUpdateWidget(covariant _PdfViewerSection oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.state.viewerReloadToken != oldWidget.state.viewerReloadToken &&
        !_shouldShowPlaceholder()) {
      unawaited(_loadPdf());
    }
  }

  bool _shouldShowPlaceholder() {
    if (widget.state.pdfUrl.isEmpty) return true;
    if (kIsWeb) return false;
    final platform = defaultTargetPlatform;
    return platform != TargetPlatform.android && platform != TargetPlatform.iOS;
  }

  Future<void> _loadPdf() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      if (widget.state.pdfUrl.isEmpty) {
        setState(() {
          _filePath = null;
          _loading = false;
        });
        return;
      }
      if (kIsWeb) {
        setState(() => _loading = false);
        return;
      }
      final bytes = await ref
          .read(documentoEmitidoProvider(widget.documentId).notifier)
          .fetchPdfBytes();
      if (bytes == null || bytes.isEmpty) {
        throw StateError('No se pudo cargar el PDF.');
      }
      final directory = await getTemporaryDirectory();
      final file = io.File(
          '${directory.path}/document_${widget.documentId}_${widget.state.viewerReloadToken}.pdf');
      await file.writeAsBytes(bytes, flush: true);
      setState(() {
        _filePath = file.path;
        _loading = false;
      });
    } catch (error) {
      setState(() {
        _error = error;
        _filePath = null;
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (widget.state.pdfUrl.isEmpty) {
      return const Center(
          child: Text('El PDF se mostrara aqui cuando este disponible.'));
    }
    if (kIsWeb) {
      return HtmlElementView(
        viewType: _registerPdfView(widget.state),
        key: ValueKey<int>(widget.state.viewerReloadToken),
      );
    }
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(child: Text('Error al cargar PDF: $_error'));
    }
    if (_filePath == null) {
      return const Center(child: Text('Descargando PDF...'));
    }
    return PDFView(
      key: ValueKey<String>(_filePath!),
      filePath: _filePath!,
      enableSwipe: true,
      swipeHorizontal: true,
      autoSpacing: true,
    );
  }

  String _registerPdfView(DocumentWatcherState state) {
    final viewType =
        'billing-pdf-${widget.documentId}-${state.viewerReloadToken}';
    if (!_registeredViewTypes.contains(viewType)) {
      registerPdfViewFactory(
        viewType,
        (int viewId) {
          final container = html.DivElement()
            ..style.width = '100%'
            ..style.height = '100%';
          final embed = html.Element.tag('embed')
            ..setAttribute('src', state.pdfUrl)
            ..setAttribute('type', 'application/pdf')
            ..style.border = 'none'
            ..style.width = '100%'
            ..style.height = '100%';
          container.children = <html.Element>[embed];
          return container;
        },
      );
      _registeredViewTypes.add(viewType);
    }
    return viewType;
  }
}

class _DocumentoErrorView extends StatelessWidget {
  const _DocumentoErrorView({required this.error, required this.onRetry});

  final Object error;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          Text(
            'No se pudo cargar el documento.\n$error',
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: onRetry,
            child: const Text('Reintentar'),
          ),
        ],
      ),
    );
  }
}

void _downloadWeb(Uint8List bytes, String filename) {
  final blob = html.Blob(<Uint8List>[bytes], 'application/pdf');
  final url = html.Url.createObjectUrlFromBlob(blob);
  final anchor = html.AnchorElement(href: url)..download = filename;
  anchor.click();
  html.Url.revokeObjectUrl(url);
}

String _buildFilename(DocumentWatcherState state) {
  final number = state.document.number ?? state.document.provisionalNumber;
  return 'documento_$number';
}
