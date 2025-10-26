import 'dart:convert';

import 'package:uuid/uuid.dart';

const Uuid _uuid = Uuid();

enum DocumentType {
  factura,
  boleta,
  cotizacion,
  comprobanteEntrega,
}

extension DocumentTypeX on DocumentType {
  String get wireValue {
    switch (this) {
      case DocumentType.factura:
        return 'FACTURA';
      case DocumentType.boleta:
        return 'BOLETA';
      case DocumentType.cotizacion:
        return 'COTIZACION';
      case DocumentType.comprobanteEntrega:
        return 'COMPROBANTE_ENTREGA';
    }
  }

  String get label {
    switch (this) {
      case DocumentType.factura:
        return 'Factura';
      case DocumentType.boleta:
        return 'Boleta';
      case DocumentType.cotizacion:
        return 'Cotizacion';
      case DocumentType.comprobanteEntrega:
        return 'Comprobante de entrega';
    }
  }

  bool get isFiscal =>
      this == DocumentType.factura || this == DocumentType.boleta;
}

DocumentType documentTypeFromWireValue(String value) {
  return DocumentType.values.firstWhere(
    (type) => type.wireValue == value.toUpperCase(),
    orElse: () => DocumentType.factura,
  );
}

enum TaxMode {
  gravada,
  exonerada,
  mixta,
}

extension TaxModeX on TaxMode {
  String get wireValue {
    switch (this) {
      case TaxMode.gravada:
        return 'GRAVADA';
      case TaxMode.exonerada:
        return 'EXONERADA';
      case TaxMode.mixta:
        return 'MIXTA';
    }
  }

  String get label {
    switch (this) {
      case TaxMode.gravada:
        return 'Gravada';
      case TaxMode.exonerada:
        return 'Exonerada';
      case TaxMode.mixta:
        return 'Mixta';
    }
  }
}

TaxMode taxModeFromWireValue(String value) {
  return TaxMode.values.firstWhere(
    (mode) => mode.wireValue == value.toUpperCase(),
    orElse: () => TaxMode.gravada,
  );
}

enum DocumentStatus {
  offlinePending,
  sent,
  accepted,
  rejected,
  cancelled,
  unknown,
}

extension DocumentStatusX on DocumentStatus {
  String get wireValue {
    switch (this) {
      case DocumentStatus.offlinePending:
        return 'OFFLINE_PENDING';
      case DocumentStatus.sent:
        return 'SENT';
      case DocumentStatus.accepted:
        return 'ACCEPTED';
      case DocumentStatus.rejected:
        return 'REJECTED';
      case DocumentStatus.cancelled:
        return 'CANCELLED';
      case DocumentStatus.unknown:
        return 'UNKNOWN';
    }
  }

  bool get isTerminal =>
      this == DocumentStatus.accepted ||
      this == DocumentStatus.rejected ||
      this == DocumentStatus.cancelled;
}

DocumentStatus documentStatusFromString(String value) {
  return DocumentStatus.values.firstWhere(
    (status) => status.wireValue == value.toUpperCase(),
    orElse: () => DocumentStatus.unknown,
  );
}

enum DocumentVersion {
  local,
  official,
  unknown,
}

extension DocumentVersionX on DocumentVersion {
  String get wireValue {
    switch (this) {
      case DocumentVersion.local:
        return 'LOCAL';
      case DocumentVersion.official:
        return 'OFFICIAL';
      case DocumentVersion.unknown:
        return 'UNKNOWN';
    }
  }
}

DocumentVersion documentVersionFromString(String value) {
  return DocumentVersion.values.firstWhere(
    (version) => version.wireValue == value.toUpperCase(),
    orElse: () => DocumentVersion.unknown,
  );
}

class BillingDocument {
  const BillingDocument({
    required this.id,
    required this.status,
    required this.version,
    required this.pdfUrl,
    required this.provisionalNumber,
    required this.history,
    this.number,
    this.payload,
    this.updatedAt,
  });

  factory BillingDocument.fromJson(Map<String, dynamic> json) {
    final history = <String, String>{};
    final historyJson =
        json['history'] as Map<String, dynamic>? ?? const <String, dynamic>{};
    for (final entry in historyJson.entries) {
      final value = entry.value is Map<String, dynamic>
          ? (entry.value as Map<String, dynamic>)['number']
          : entry.value;
      if (value == null) continue;
      history[entry.key] = value.toString();
    }
    final timestamps = json['updatedAt'] ?? json['updated_at'];
    DateTime? updatedAt;
    if (timestamps is String) {
      updatedAt = DateTime.tryParse(timestamps);
    }
    return BillingDocument(
      id: json['id']?.toString() ?? '',
      status: documentStatusFromString(json['status']?.toString() ?? ''),
      version: documentVersionFromString(json['version']?.toString() ?? ''),
      pdfUrl: _resolvePdfUrl(json), // FIX: resolve PDF URL from links fallbacks.
      provisionalNumber: json['provisionalNumber']?.toString() ??
          json['provisional_number']?.toString() ??
          '',
      number: json['number']?.toString(),
      history: history,
      payload: json['payload'],
      updatedAt: updatedAt,
    );
  }

  final String id;
  final DocumentStatus status;
  final DocumentVersion version;
  final String pdfUrl;
  final String provisionalNumber;
  final Map<String, String> history;
  final String? number;
  final Object? payload;
  final DateTime? updatedAt;

  bool get isContingency =>
      status == DocumentStatus.offlinePending ||
      version == DocumentVersion.local;

  BillingDocument copyWith({
    DocumentStatus? status,
    DocumentVersion? version,
    String? pdfUrl,
    String? provisionalNumber,
    String? number,
    Map<String, String>? history,
    Object? payload,
    DateTime? updatedAt,
  }) {
    return BillingDocument(
      id: id,
      status: status ?? this.status,
      version: version ?? this.version,
      pdfUrl: pdfUrl ?? this.pdfUrl,
      provisionalNumber: provisionalNumber ?? this.provisionalNumber,
      number: number ?? this.number,
      history: history ?? this.history,
      payload: payload ?? this.payload,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }
}

String _resolvePdfUrl(Map<String, dynamic> json) {
  // FIX: resolve PDF URL using provided link fallbacks.
  final direct = json['pdfUrl'] ?? json['pdf_url'] ?? json['pdf'];
  if (direct is String && direct.isNotEmpty) {
    return direct;
  }
  final links = json['links'];
  if (links is Map<String, dynamic>) {
    final official = links['officialPdf'] ??
        links['official_pdf'] ??
        links['pdf'];
    if (official is String && official.isNotEmpty) {
      return official;
    }
    final local = links['localPdf'] ?? links['local_pdf'];
    if (local is String && local.isNotEmpty) {
      return local;
    }
  }
  return '';
}

class IssueInvoiceRequest {
  IssueInvoiceRequest({
    required this.documentType,
    required this.taxMode,
    required this.sale,
    this.idempotencyKey,
    required this.forceOffline,
    this.connectivityHint,
    this.notes,
  });

  final DocumentType documentType;
  final TaxMode taxMode;
  final SaleDTO sale;
  final bool forceOffline;
  final String? connectivityHint;
  final String? notes;
  String? idempotencyKey;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'documentType': documentType.wireValue,
      'taxMode': taxMode.wireValue,
      'sale': sale.toJson(), // FIX: include full sale payload for backend contract.
      if (idempotencyKey?.isNotEmpty ?? false) 'idempotencyKey': idempotencyKey,
      'forceOffline': forceOffline,
      if (connectivityHint?.isNotEmpty ?? false)
        'connectivityHint': connectivityHint,
      if (notes?.isNotEmpty ?? false) 'notes': notes,
    };
  }
}

class SaleRequest {
  SaleRequest({
    required this.documentType,
    required this.taxMode,
    required this.forceOffline,
    required this.sale,
    this.connectivityHint,
    this.notes,
    this.idempotencyKey,
  });

  final DocumentType documentType;
  final TaxMode taxMode;
  final bool forceOffline;
  final SaleDTO sale;
  final String? connectivityHint;
  final String? notes;
  String? idempotencyKey;

  Map<String, dynamic> toJson() {
    final payloadSale = notes != null && notes!.isNotEmpty
        ? sale.copyWith(notes: notes)
        : sale;
    final request = IssueInvoiceRequest(
      documentType: documentType,
      taxMode: taxMode,
      sale: payloadSale,
      idempotencyKey: idempotencyKey,
      forceOffline: forceOffline,
      connectivityHint: connectivityHint,
      notes: notes,
    );
    return request.toJson();
  }

  SaleRequest copyWith({
    DocumentType? documentType,
    TaxMode? taxMode,
    bool? forceOffline,
    SaleDTO? sale,
    String? connectivityHint,
    Object? notes = _unset,
    Object? idempotencyKey = _unset,
  }) {
    return SaleRequest(
      documentType: documentType ?? this.documentType,
      taxMode: taxMode ?? this.taxMode,
      forceOffline: forceOffline ?? this.forceOffline,
      sale: sale ?? this.sale,
      connectivityHint: connectivityHint ?? this.connectivityHint,
      notes: notes == _unset ? this.notes : notes as String?,
      idempotencyKey: idempotencyKey == _unset
          ? this.idempotencyKey
          : idempotencyKey as String?,
    );
  }

  static SaleRequest sample() {
    final items = <SaleItemRequest>[
      const SaleItemRequest(description: 'Articulo', quantity: 1, unitPrice: 0),
    ];
    final sale = SaleDTO.fromLegacy(
      id: _uuid.v4(),
      items: items,
      taxMode: TaxMode.gravada,
      customerName: '',
      customerTaxId: '',
    );
    return SaleRequest(
      documentType: DocumentType.factura,
      taxMode: TaxMode.gravada,
      forceOffline: false,
      sale: sale,
    );
  }
}

class SaleDTO {
  const SaleDTO({
    required this.id,
    required this.items,
    required this.net,
    required this.vat,
    required this.total,
    this.issueDate,
    this.seller,
    this.buyer,
    this.pointOfSale,
    this.deviceId,
    this.customerName,
    this.customerTaxId,
    this.notes,
  });

  factory SaleDTO.fromLegacy({
    required String id,
    required List<SaleItemRequest> items,
    required TaxMode taxMode,
    String? customerName,
    String? customerTaxId,
    DateTime? issueDate,
    SalePartyDTO? seller,
    SalePartyDTO? buyer,
    String? pointOfSale,
    String? deviceId,
    String? notes,
  }) {
    final totals = _calculateTotals(items, taxMode);
    return SaleDTO(
      id: id,
      items: items,
      net: totals.net,
      vat: totals.vat,
      total: totals.total,
      issueDate: issueDate,
      seller: seller,
      buyer: buyer ?? SalePartyDTO(name: customerName, taxId: customerTaxId),
      pointOfSale: pointOfSale,
      deviceId: deviceId,
      customerName: customerName,
      customerTaxId: customerTaxId,
      notes: notes,
    );
  }

  final String id;
  final List<SaleItemRequest> items;
  final double net;
  final double vat;
  final double total;
  final DateTime? issueDate;
  final SalePartyDTO? seller;
  final SalePartyDTO? buyer;
  final String? pointOfSale;
  final String? deviceId;
  final String? customerName;
  final String? customerTaxId;
  final String? notes;

  Map<String, dynamic> toJson() {
    final data = <String, dynamic>{
      'id': id,
      'items': items.map((item) => item.toJson()).toList(),
      'net': _formatCurrency(net),
      'vat': _formatCurrency(vat),
      'total': _formatCurrency(total),
    };
    if (customerName?.isNotEmpty ?? false) {
      data['customerName'] = customerName;
    }
    if (customerTaxId?.isNotEmpty ?? false) {
      data['customerTaxId'] = customerTaxId;
    }
    if (pointOfSale?.isNotEmpty ?? false) {
      data['pointOfSale'] = pointOfSale;
    }
    if (deviceId?.isNotEmpty ?? false) {
      data['deviceId'] = deviceId;
    }
    if (issueDate != null) {
      data['issueDate'] = issueDate!.toIso8601String();
    }
    if (notes?.isNotEmpty ?? false) {
      data['notes'] = notes;
    }
    final sellerJson = seller?.toJson();
    if (sellerJson != null && sellerJson.isNotEmpty) {
      data['seller'] = sellerJson;
    }
    final buyerJson = buyer?.toJson();
    if (buyerJson != null && buyerJson.isNotEmpty) {
      data['buyer'] = buyerJson;
    }
    return data;
  }

  SaleDTO copyWith({
    String? id,
    List<SaleItemRequest>? items,
    double? net,
    double? vat,
    double? total,
    Object? issueDate = _unset,
    Object? seller = _unset,
    Object? buyer = _unset,
    Object? pointOfSale = _unset,
    Object? deviceId = _unset,
    Object? customerName = _unset,
    Object? customerTaxId = _unset,
    Object? notes = _unset,
  }) {
    return SaleDTO(
      id: id ?? this.id,
      items: items ?? this.items,
      net: net ?? this.net,
      vat: vat ?? this.vat,
      total: total ?? this.total,
      issueDate:
          issueDate == _unset ? this.issueDate : issueDate as DateTime?,
      seller: seller == _unset ? this.seller : seller as SalePartyDTO?,
      buyer: buyer == _unset ? this.buyer : buyer as SalePartyDTO?,
      pointOfSale: pointOfSale == _unset
          ? this.pointOfSale
          : pointOfSale as String?,
      deviceId:
          deviceId == _unset ? this.deviceId : deviceId as String?,
      customerName: customerName == _unset
          ? this.customerName
          : customerName as String?,
      customerTaxId: customerTaxId == _unset
          ? this.customerTaxId
          : customerTaxId as String?,
      notes: notes == _unset ? this.notes : notes as String?,
    );
  }
}

class SalePartyDTO {
  const SalePartyDTO({
    this.id,
    this.name,
    this.taxId,
    this.email,
    this.phone,
    this.address,
  });

  final String? id;
  final String? name;
  final String? taxId;
  final String? email;
  final String? phone;
  final String? address;

  Map<String, dynamic> toJson() {
    final data = <String, dynamic>{};
    if (id?.isNotEmpty ?? false) {
      data['id'] = id;
    }
    if (name?.isNotEmpty ?? false) {
      data['name'] = name;
    }
    if (taxId?.isNotEmpty ?? false) {
      data['taxId'] = taxId;
    }
    if (email?.isNotEmpty ?? false) {
      data['email'] = email;
    }
    if (phone?.isNotEmpty ?? false) {
      data['phone'] = phone;
    }
    if (address?.isNotEmpty ?? false) {
      data['address'] = address;
    }
    return data;
  }

  SalePartyDTO copyWith({
    Object? id = _unset,
    Object? name = _unset,
    Object? taxId = _unset,
    Object? email = _unset,
    Object? phone = _unset,
    Object? address = _unset,
  }) {
    return SalePartyDTO(
      id: id == _unset ? this.id : id as String?,
      name: name == _unset ? this.name : name as String?,
      taxId: taxId == _unset ? this.taxId : taxId as String?,
      email: email == _unset ? this.email : email as String?,
      phone: phone == _unset ? this.phone : phone as String?,
      address: address == _unset ? this.address : address as String?,
    );
  }
}

class SaleItemRequest {
  const SaleItemRequest({
    this.productId,
    required this.description,
    required this.quantity,
    required this.unitPrice,
    this.discount = 0,
  });

  final String? productId;
  final String description;
  final double quantity;
  final double unitPrice;
  final double discount;

  Map<String, dynamic> toJson() {
    final data = <String, dynamic>{
      if (productId?.isNotEmpty ?? false) 'productId': productId,
      'description': description,
      'quantity': _formatQuantity(quantity),
      'unitPrice': _formatCurrency(unitPrice),
      'discount': _formatCurrency(discount),
    };
    return data;
  }

  double get total {
    final raw = (quantity * unitPrice) - discount;
    return raw < 0 ? 0 : raw;
  }

  SaleItemRequest copyWith({
    Object? productId = _unset,
    String? description,
    double? quantity,
    double? unitPrice,
    double? discount,
  }) {
    return SaleItemRequest(
      productId:
          productId == _unset ? this.productId : productId as String?,
      description: description ?? this.description,
      quantity: quantity ?? this.quantity,
      unitPrice: unitPrice ?? this.unitPrice,
      discount: discount ?? this.discount,
    );
  }
}

class SaleTotals {
  const SaleTotals({
    required this.net,
    required this.vat,
    required this.total,
  });

  final double net;
  final double vat;
  final double total;
}

SaleTotals _calculateTotals(List<SaleItemRequest> items, TaxMode taxMode) {
  final total = _roundCurrency(
    items.fold<double>(0, (sum, item) => sum + item.total),
  );
  double vatRate;
  switch (taxMode) {
    case TaxMode.gravada:
    case TaxMode.mixta:
      vatRate = 0.19;
      break;
    case TaxMode.exonerada:
      vatRate = 0;
      break;
  }
  final net = vatRate > 0
      ? _roundCurrency(total / (1 + vatRate))
      : total;
  final vat = _roundCurrency(total - net);
  return SaleTotals(net: net, vat: vat, total: total);
}

double _roundCurrency(double value) {
  return (value * 100).roundToDouble() / 100;
}

String _formatCurrency(double value) {
  return _roundCurrency(value).toStringAsFixed(2);
}

String _formatQuantity(double value) {
  final rounded = _roundCurrency(value);
  return rounded == rounded.truncateToDouble()
      ? rounded.toInt().toString()
      : rounded.toStringAsFixed(2);
}

const Object _unset = Object();

String encodeHistory(Map<String, String> history) => jsonEncode(history);

Map<String, String> decodeHistory(String value) {
  final decoded = jsonDecode(value) as Map<String, dynamic>;
  return decoded.map((key, result) => MapEntry(key, result.toString()));
}
