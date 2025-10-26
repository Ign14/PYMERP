import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/billing_repository.dart';
import '../domain/billing_models.dart';
import 'billing_providers.dart';

final registrarVentaControllerProvider =
    AutoDisposeNotifierProvider<RegistrarVentaController, RegistrarVentaState>(
  RegistrarVentaController.new,
);

class RegistrarVentaState {
  const RegistrarVentaState({
    required this.isSubmitting,
    this.error,
    this.lastDocumentId,
  });

  const RegistrarVentaState.idle() : this(isSubmitting: false);

  final bool isSubmitting;
  final Object? error;
  final String? lastDocumentId;

  RegistrarVentaState copyWith({
    bool? isSubmitting,
    Object? error = _sentinel,
    String? lastDocumentId,
  }) {
    return RegistrarVentaState(
      isSubmitting: isSubmitting ?? this.isSubmitting,
      error: error == _sentinel ? this.error : error,
      lastDocumentId: lastDocumentId ?? this.lastDocumentId,
    );
  }
}

class RegistrarVentaController
    extends AutoDisposeNotifier<RegistrarVentaState> {
  late final BillingDataSource _repository;

  @override
  RegistrarVentaState build() {
    _repository = ref.watch(billingRepositoryProvider);
    return const RegistrarVentaState.idle();
  }

  Future<String?> submit(SaleRequest request) async {
    if (state.isSubmitting) return null;
    state =
        state.copyWith(isSubmitting: true, error: null, lastDocumentId: null);
    try {
      final documentId = await _repository.submitSale(request);
      state = state.copyWith(
          isSubmitting: false, lastDocumentId: documentId, error: null);
      return documentId;
    } catch (error) {
      state = state.copyWith(
          isSubmitting: false, error: error, lastDocumentId: null);
      return null;
    }
  }

  void clearError() {
    if (state.error != null) {
      state = state.copyWith(error: null);
    }
  }
}

const Object _sentinel = Object();
