import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../application/registrar_venta_notifier.dart';
import '../domain/billing_models.dart';
import 'documento_emitido_view.dart';

class RegistrarVentaPage extends ConsumerStatefulWidget {
  const RegistrarVentaPage({super.key});

  @override
  ConsumerState<RegistrarVentaPage> createState() => _RegistrarVentaPageState();
}

class _RegistrarVentaPageState extends ConsumerState<RegistrarVentaPage> {
  static const Uuid _uuid = Uuid();

  final _formKey = GlobalKey<FormState>();
  final _customerNameController = TextEditingController();
  final _customerTaxIdController = TextEditingController();
  final _notesController = TextEditingController();
  final List<_SaleItemFields> _items = <_SaleItemFields>[_SaleItemFields()];

  DocumentType _selectedDocumentType = DocumentType.factura;
  TaxMode _selectedTaxMode = TaxMode.gravada;
  bool _forceOffline = false;

  @override
  void dispose() {
    _customerNameController.dispose();
    _customerTaxIdController.dispose();
    _notesController.dispose();
    for (final item in _items) {
      item.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<RegistrarVentaState>(
      registrarVentaControllerProvider,
      (previous, next) {
        if (previous?.error != next.error && next.error != null && mounted) {
          final message = next.error.toString();
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(message)),
          );
        }
      },
    );

    final state = ref.watch(registrarVentaControllerProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Registrar venta'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              const Text(
                'Datos del cliente',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _customerNameController,
                decoration:
                    const InputDecoration(labelText: 'Razon social / Nombre'),
                validator: (value) => value == null || value.isEmpty
                    ? 'Ingresa el nombre del cliente'
                    : null,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _customerTaxIdController,
                decoration: const InputDecoration(labelText: 'RUC / DNI'),
                validator: (value) => value == null || value.isEmpty
                    ? 'Ingresa el documento del cliente'
                    : null,
              ),
              const SizedBox(height: 24),
              Row(
                children: <Widget>[
                  Expanded(
                    child: DropdownButtonFormField<DocumentType>(
                      // ignore: deprecated_member_use
                      value: _selectedDocumentType,
                      decoration:
                          const InputDecoration(labelText: 'Tipo de documento'),
                      onChanged: (value) {
                        if (value != null) {
                          setState(() => _selectedDocumentType = value);
                        }
                      },
                      items: DocumentType.values
                          .map(
                            (type) => DropdownMenuItem<DocumentType>(
                              value: type,
                              child: Text(type.label),
                            ),
                          )
                          .toList(),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: DropdownButtonFormField<TaxMode>(
                      // ignore: deprecated_member_use
                      value: _selectedTaxMode,
                      decoration:
                          const InputDecoration(labelText: 'Tratamiento IGV'),
                      onChanged: (value) {
                        if (value != null) {
                          setState(() => _selectedTaxMode = value);
                        }
                      },
                      items: TaxMode.values
                          .map(
                            (mode) => DropdownMenuItem<TaxMode>(
                              value: mode,
                              child: Text(mode.label),
                            ),
                          )
                          .toList(),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              SwitchListTile(
                value: _forceOffline,
                onChanged: (value) => setState(() => _forceOffline = value),
                title: const Text('Forzar modo offline'),
                subtitle: const Text(
                    'Emite en contingencia y sincroniza al restablecerse la conexion.'),
              ),
              const SizedBox(height: 24),
              const Text(
                'Detalle de la venta',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 16),
              ..._items.asMap().entries.map(
                (entry) {
                  final index = entry.key;
                  final fields = entry.value;
                  return Padding(
                    padding: EdgeInsets.only(
                        bottom: index == _items.length - 1 ? 12 : 20),
                    child: _SaleItemTile(
                      fields: fields,
                      onRemove: _items.length == 1
                          ? null
                          : () {
                              setState(() {
                                _items.removeAt(index);
                              });
                            },
                    ),
                  );
                },
              ),
              Align(
                alignment: Alignment.centerLeft,
                child: TextButton.icon(
                  onPressed: () {
                    setState(() => _items.add(_SaleItemFields()));
                  },
                  icon: const Icon(Icons.add),
                  label: const Text('Agregar item'),
                ),
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _notesController,
                decoration: const InputDecoration(
                  labelText: 'Observaciones',
                  alignLabelWithHint: true,
                ),
                maxLines: 4,
              ),
              const SizedBox(height: 32),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: state.isSubmitting ? null : _onSubmit,
                  icon: state.isSubmitting
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2))
                      : const Icon(Icons.check_circle),
                  label: Text(state.isSubmitting ? 'Enviando...' : 'Confirmar'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _onSubmit() async {
    if (!_formKey.currentState!.validate()) return;
    final items = _items
        .where((item) => item.descriptionController.text.trim().isNotEmpty)
        .map(
          (item) => SaleItemRequest(
            description: item.descriptionController.text.trim(),
            quantity: double.tryParse(
                    item.quantityController.text.replaceAll(',', '.')) ??
                1,
            unitPrice: double.tryParse(
                    item.unitPriceController.text.replaceAll(',', '.')) ??
                0,
          ),
        )
        .where((item) => item.quantity > 0 && item.unitPrice >= 0)
        .toList();

    if (items.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Agrega al menos un item valido.')),
      );
      return;
    }

    final customerName = _customerNameController.text.trim();
    final customerTaxId = _customerTaxIdController.text.trim();
    final notesText = _notesController.text.trim();
    final normalizedNotes = notesText.isEmpty ? null : notesText;
    final sale = SaleDTO.fromLegacy(
      id: _uuid.v4(),
      items: items,
      taxMode: _selectedTaxMode,
      customerName: customerName,
      customerTaxId: customerTaxId,
      notes: normalizedNotes,
    );
    final request = SaleRequest(
      documentType: _selectedDocumentType,
      taxMode: _selectedTaxMode,
      forceOffline: _forceOffline,
      sale: sale,
      notes: normalizedNotes,
    );

    final controller = ref.read(registrarVentaControllerProvider.notifier);
    final documentId = await controller.submit(request);

    if (!mounted) return;
    if (documentId != null) {
      Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (context) => DocumentoEmitidoView(documentId: documentId),
        ),
      );
    }
  }
}

class _SaleItemTile extends StatelessWidget {
  const _SaleItemTile({required this.fields, this.onRemove});

  final _SaleItemFields fields;
  final VoidCallback? onRemove;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: <Widget>[
            TextFormField(
              controller: fields.descriptionController,
              decoration: const InputDecoration(labelText: 'Descripcion'),
              validator: (value) => value == null || value.isEmpty
                  ? 'Agrega una descripcion'
                  : null,
            ),
            const SizedBox(height: 12),
            Row(
              children: <Widget>[
                Expanded(
                  child: TextFormField(
                    controller: fields.quantityController,
                    decoration: const InputDecoration(labelText: 'Cantidad'),
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                    validator: (value) {
                      final parsed =
                          double.tryParse((value ?? '').replaceAll(',', '.'));
                      if (parsed == null || parsed <= 0) {
                        return 'Cantidad > 0';
                      }
                      return null;
                    },
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: TextFormField(
                    controller: fields.unitPriceController,
                    decoration:
                        const InputDecoration(labelText: 'Precio unitario'),
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                    validator: (value) {
                      final parsed =
                          double.tryParse((value ?? '').replaceAll(',', '.'));
                      if (parsed == null || parsed < 0) {
                        return 'Precio >= 0';
                      }
                      return null;
                    },
                  ),
                ),
              ],
            ),
            if (onRemove != null) ...<Widget>[
              const SizedBox(height: 12),
              Align(
                alignment: Alignment.centerRight,
                child: IconButton(
                  onPressed: onRemove,
                  icon: const Icon(Icons.delete_outline),
                  tooltip: 'Quitar item',
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _SaleItemFields {
  _SaleItemFields()
      : descriptionController = TextEditingController(),
        quantityController = TextEditingController(text: '1'),
        unitPriceController = TextEditingController(text: '0');

  final TextEditingController descriptionController;
  final TextEditingController quantityController;
  final TextEditingController unitPriceController;

  void dispose() {
    descriptionController.dispose();
    quantityController.dispose();
    unitPriceController.dispose();
  }
}
