import 'package:dio/dio.dart';
import '../core/api_client.dart';
import '../core/pagination.dart';
import '../domain/customer.dart';

class CustomerService {
  final ApiClient api;
  CustomerService(this.api);

  Future<Page<Customer>> list({required int page, int pageSize = 20}) async {
    final resp = await api.dio.get('/v1/customers', queryParameters: {
      'page': page,
      'size': pageSize,
    });
    final data = resp.data as Map<String, dynamic>;
    final rawItems = (data['content'] ?? data['items']) as List? ?? const [];
    final items = rawItems.map((e) => Customer.fromJson(e as Map<String, dynamic>)).toList();
    final currentPage = (data['number'] as num?)?.toInt() ?? page;
    final totalPages = (data['totalPages'] as num?)?.toInt();
    final hasNext = totalPages != null ? currentPage < totalPages - 1 : (data['hasNext'] == true);
    final size = (data['size'] as num?)?.toInt() ?? pageSize;
    return Page(items: items, pageNumber: currentPage, pageSize: size, hasNext: hasNext);
  }

  Future<Customer> create(Map<String, dynamic> payload) async {
    final body = Map<String, dynamic>.from(payload)..removeWhere((key, value) => value == null);
    final resp = await api.dio.post('/v1/customers', data: body);
    return Customer.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<Customer> update(String id, Map<String, dynamic> payload) async {
    final body = Map<String, dynamic>.from(payload)..removeWhere((key, value) => value == null);
    final resp = await api.dio.put('/v1/customers/', data: body);
    return Customer.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<void> delete(String id) async {
    await api.dio.delete('/v1/customers/');
  }
}
