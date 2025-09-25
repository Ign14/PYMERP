import '../../../core/api_client.dart';
import '../../../core/pagination.dart';
import '../../../domain/customer.dart';

abstract class CustomersDataSource {
  Future<Page<Customer>> list({int page = 0, int pageSize = 20});
}

class CustomersRepository implements CustomersDataSource {
  CustomersRepository(this._client);

  final ApiClient _client;

  @override
  Future<Page<Customer>> list({int page = 0, int pageSize = 20}) async {
    final response = await _client.dio.get<Map<String, dynamic>>(
      '/api/v1/customers',
      queryParameters: {
        'page': page,
        'size': pageSize,
      },
    );
    final payload = response.data ?? const <String, dynamic>{};
    final items = (payload['data'] as List<dynamic>? ?? const <dynamic>[])
        .map((json) => Customer.fromJson(json as Map<String, dynamic>))
        .toList();
    final total = (payload['total'] as num?)?.toInt() ?? items.length;
    final responsePage = payload['page'] as int? ?? page;
    final responseSize = payload['size'] as int? ?? pageSize;
    final hasNext = (responsePage + 1) * responseSize < total;
    return Page<Customer>(items: items, pageNumber: responsePage, pageSize: responseSize, hasNext: hasNext);
  }
}
