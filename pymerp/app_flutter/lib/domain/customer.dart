import 'package:freezed_annotation/freezed_annotation.dart';

part 'customer.freezed.dart';
part 'customer.g.dart';

// ignore_for_file: invalid_annotation_target

double? _toDouble(Object? value) {
  if (value == null) return null;
  if (value is num) return value.toDouble();
  if (value is String) return double.tryParse(value.replaceAll(',', '.'));
  return null;
}

Object? _doubleToJson(double? value) => value;

@freezed
class Customer with _$Customer {
  @JsonSerializable(includeIfNull: false)
  const factory Customer({
    required String id,
    required String name,
    String? email,
    String? phone,
    @JsonKey(fromJson: _toDouble, toJson: _doubleToJson, includeIfNull: false) double? lat,
    @JsonKey(fromJson: _toDouble, toJson: _doubleToJson, includeIfNull: false) double? lng,
    String? address,
    DateTime? updatedAt,
  }) = _Customer;

  factory Customer.fromJson(Map<String, dynamic> json) => _$CustomerFromJson(json);
}
