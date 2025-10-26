import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/api_client_provider.dart';
import '../data/billing_repository.dart';

final billingRepositoryProvider = Provider<BillingDataSource>((ref) {
  final client = ref.watch(apiClientProvider);
  return BillingRepository(client);
});
