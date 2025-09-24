import 'package:flutter_test/flutter_test.dart';
import 'package:pymerp_app/core/pagination.dart';
import 'package:pymerp_app/domain/customer.dart';
import 'package:pymerp_app/features/customers/customers_controller.dart';
import 'package:pymerp_app/services/customer_service.dart';
import 'package:riverpod/riverpod.dart';
import 'package:pymerp_app/core/config.dart';
import 'package:pymerp_app/core/api_client.dart';

class _FakeCustomerService extends CustomerService {
  _FakeCustomerService() : super(ApiClient(const AppConfig(baseUrl: 'http://localhost', companyId: 'dev', useHttps: false)));

  final List<List<Customer>> _pages = [
    List.generate(3, (i) => Customer(id: 'id_', name: 'Customer ')),
    List.generate(2, (i) => Customer(id: 'id_', name: 'Customer ')),
  ];

  @override
  Future<Page<Customer>> list({required int page, int pageSize = 20}) async {
    if (page >= _pages.length) {
      return Page(items: const [], pageNumber: page, pageSize: pageSize, hasNext: false);
    }
    final items = _pages[page];
    final hasNext = page < _pages.length - 1;
    return Page(items: items, pageNumber: page, pageSize: pageSize, hasNext: hasNext);
  }
}

void main() {
  test('CustomersState paginates correctly', () async {
    final container = ProviderContainer(overrides: [
      customerServiceProvider.overrideWithValue(_FakeCustomerService()),
    ]);
    addTearDown(container.dispose);

    final notifier = container.read(customersStateProvider.notifier);

    await notifier.loadFirst();
    final state1 = container.read(customersStateProvider);
    expect(state1.value?.items.length, 3);
    expect(state1.value?.hasNext, isTrue);

    await notifier.loadMore();
    final state2 = container.read(customersStateProvider);
    expect(state2.value?.items.length, 5);
    expect(state2.value?.hasNext, isFalse);
  });
}
