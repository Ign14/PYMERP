import 'package:flutter_test/flutter_test.dart';
import 'package:pymerp_app/domain/customer.dart';

void main() {
  test('Customer json roundtrip with coordinates', () {
    final c = Customer(id: '1', name: 'Acme', lat: 10.5, lng: -70.123456);
    final j = c.toJson();
    expect(j['lat'], 10.5);
    expect(j['lng'], -70.123456);
    final c2 = Customer.fromJson(j);
    expect(c2.name, 'Acme');
    expect(c2.lat, 10.5);
    expect(c2.lng, -70.123456);
  });

  test('Customer json omits null coordinates', () {
    final c = Customer(id: '2', name: 'NoGeo');
    final j = c.toJson();
    expect(j.containsKey('lat'), isFalse);
    expect(j.containsKey('lng'), isFalse);
    final decoded = Customer.fromJson({'id': '2', 'name': 'NoGeo', 'lat': null});
    expect(decoded.lat, isNull);
    expect(decoded.lng, isNull);
  });
}
