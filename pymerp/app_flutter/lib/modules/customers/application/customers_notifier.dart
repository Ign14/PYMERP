import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/api_client_provider.dart';
import '../../../domain/customer.dart';
import '../data/customers_repository.dart';

final customersRepositoryProvider = Provider<CustomersDataSource>((ref) {
  final client = ref.watch(apiClientProvider);
  return CustomersRepository(client);
});

final customersNotifierProvider = AutoDisposeNotifierProvider<CustomersNotifier, CustomersState>(() {
  return CustomersNotifier();
});

class CustomersState {
  const CustomersState({
    required this.items,
    required this.isLoading,
    required this.isLoadingMore,
    required this.hasNext,
    this.error,
  });

  factory CustomersState.initial() => const CustomersState(
        items: <Customer>[],
        isLoading: false,
        isLoadingMore: false,
        hasNext: true,
        error: null,
      );

  final List<Customer> items;
  final bool isLoading;
  final bool isLoadingMore;
  final bool hasNext;
  final Object? error;

  CustomersState copyWith({
    List<Customer>? items,
    bool? isLoading,
    bool? isLoadingMore,
    bool? hasNext,
    Object? error = _sentinel,
  }) {
    return CustomersState(
      items: items ?? this.items,
      isLoading: isLoading ?? this.isLoading,
      isLoadingMore: isLoadingMore ?? this.isLoadingMore,
      hasNext: hasNext ?? this.hasNext,
      error: error == _sentinel ? this.error : error,
    );
  }
}

const _sentinel = Object();

class CustomersNotifier extends AutoDisposeNotifier<CustomersState> {
  late final CustomersDataSource _repository;
  int _page = 0;
  final int _pageSize = 20;

  @override
  CustomersState build() {
    _repository = ref.watch(customersRepositoryProvider);
    return CustomersState.initial();
  }

  Future<void> loadFirst() async {
    state = state.copyWith(isLoading: true, error: null);
    _page = 0;
    await _load(page: _page, replace: true);
  }

  Future<void> loadMore() async {
    if (state.isLoadingMore || !state.hasNext) return;
    state = state.copyWith(isLoadingMore: true, error: null);
    _page += 1;
    await _load(page: _page, replace: false);
  }

  Future<void> refresh() => loadFirst();

  Future<void> _load({required int page, required bool replace}) async {
    try {
      final result = await _repository.list(page: page, pageSize: _pageSize);
      final items = replace ? result.items : [...state.items, ...result.items];
      state = state.copyWith(
        items: items,
        isLoading: false,
        isLoadingMore: false,
        hasNext: result.hasNext,
      );
    } catch (error) {
      state = state.copyWith(isLoading: false, isLoadingMore: false, error: error);
    }
  }
}
