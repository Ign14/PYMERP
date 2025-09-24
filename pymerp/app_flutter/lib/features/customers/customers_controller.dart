import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/pagination.dart';
import '../../domain/customer.dart';
import '../../services/customer_service.dart';

final customerServiceProvider = Provider<CustomerService>((ref) => throw UnimplementedError());

final customersStateProvider = StateNotifierProvider<CustomersState, AsyncValue<Page<Customer>>>((ref) {
  return CustomersState(ref.read);
});

class CustomersState extends StateNotifier<AsyncValue<Page<Customer>>> {
  CustomersState(this._read) : super(const AsyncValue.loading());
  final Reader _read;
  int _page = 0;
  bool _loadingMore = false;
  final List<Customer> _all = [];
  bool _hasNext = true;

  Future<void> loadFirst() async {
    state = const AsyncValue.loading();
    _page = 0;
    _all.clear();
    _hasNext = true;
    await _load();
  }

  Future<void> loadMore() async {
    if (_loadingMore || !_hasNext) return;
    _page++;
    await _load(append: true);
  }

  Future<void> _load({bool append = false}) async {
    _loadingMore = true;
    try {
      final svc = _read(customerServiceProvider);
      final page = await svc.list(page: _page, pageSize: 20);
      if (append) {
        _all.addAll(page.items);
        _hasNext = page.hasNext;
      } else {
        _all
          ..clear()
          ..addAll(page.items);
        _hasNext = page.hasNext;
      }
      state = AsyncValue.data(
        Page(items: List.unmodifiable(_all), pageNumber: page.pageNumber, pageSize: page.pageSize, hasNext: _hasNext),
      );
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    } finally {
      _loadingMore = false;
    }
  }
}
