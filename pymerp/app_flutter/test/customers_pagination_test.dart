import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pymerp_app/core/pagination.dart';
import 'package:pymerp_app/domain/customer.dart';
import 'package:pymerp_app/modules/customers/application/customers_notifier.dart';
import 'package:pymerp_app/modules/customers/data/customers_repository.dart';

class _FakeCustomersRepository implements CustomersDataSource {
  final List<List<Customer>> _pages = [
    List.generate(3, (index) => Customer(id: 'id_$index', name: 'Customer $index')),
    List.generate(2, (index) => Customer(id: 'id_${index + 3}', name: 'Customer ${index + 3}')),
  ];

  @override
  Future<Page<Customer>> list({int page = 0, int pageSize = 20}) async {
    if (page >= _pages.length) {
      return Page(items: const [], pageNumber: page, pageSize: pageSize, hasNext: false);
    }
    final items = _pages[page];
    final hasNext = page < _pages.length - 1;
    return Page(items: items, pageNumber: page, pageSize: pageSize, hasNext: hasNext);
  }
}

void main() {
  test('CustomersNotifier paginates correctly', () async {
    final container = ProviderContainer(
      overrides: [
        customersRepositoryProvider.overrideWithValue(_FakeCustomersRepository()),
      ],
    );
    addTearDown(container.dispose);

    final notifier = container.read(customersNotifierProvider.notifier);

    await notifier.loadFirst();
    final state1 = container.read(customersNotifierProvider);
    expect(state1.items.length, 3);
    expect(state1.hasNext, isTrue);

    await notifier.loadMore();
    final state2 = container.read(customersNotifierProvider);
    expect(state2.items.length, 5);
    expect(state2.hasNext, isFalse);
  });
}
