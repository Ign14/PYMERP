// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'customer.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_$CustomerImpl _$$CustomerImplFromJson(Map<String, dynamic> json) =>
    _$CustomerImpl(
      id: json['id'] as String,
      name: json['name'] as String,
      email: json['email'] as String?,
      phone: json['phone'] as String?,
      lat: _toDouble(json['lat']),
      lng: _toDouble(json['lng']),
      address: json['address'] as String?,
      updatedAt: json['updatedAt'] == null
          ? null
          : DateTime.parse(json['updatedAt'] as String),
    );

Map<String, dynamic> _$$CustomerImplToJson(_$CustomerImpl instance) =>
    <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      if (instance.email case final value?) 'email': value,
      if (instance.phone case final value?) 'phone': value,
      if (_doubleToJson(instance.lat) case final value?) 'lat': value,
      if (_doubleToJson(instance.lng) case final value?) 'lng': value,
      if (instance.address case final value?) 'address': value,
      if (instance.updatedAt?.toIso8601String() case final value?)
        'updatedAt': value,
    };
