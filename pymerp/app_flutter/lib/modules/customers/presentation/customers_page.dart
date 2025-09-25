import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/application/auth_notifier.dart';
import '../application/customers_notifier.dart';
import 'widgets/paginated_list_view.dart';

class CustomersPage extends ConsumerStatefulWidget {
  const CustomersPage({super.key});

  @override
  ConsumerState<CustomersPage> createState() => _CustomersPageState();
}

class _CustomersPageState extends ConsumerState<CustomersPage> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(authNotifierProvider.notifier).refreshIfNeeded();
      ref.read(customersNotifierProvider.notifier).loadFirst();
    });
  }

  @override
  Widget build(BuildContext context) {
    final customersState = ref.watch(customersNotifierProvider);
    final items = customersState.items;
    final isLoading = customersState.isLoading && items.isEmpty;
    final error = customersState.error;

    if (error is DioException && error.response?.statusCode == 401) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ref.read(authNotifierProvider.notifier).logout();
        if (mounted) Navigator.of(context).pushReplacementNamed('/login');
      });
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Clientes'),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            onPressed: () async {
              await ref.read(authNotifierProvider.notifier).logout();
              if (context.mounted) Navigator.of(context).pushReplacementNamed('/login');
            },
          ),
        ],
      ),
      body: Builder(
        builder: (context) {
          if (isLoading) {
            return const Center(child: CircularProgressIndicator());
          }
          if (customersState.error != null && items.isEmpty) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Text('No se pudo cargar la información de clientes.'),
                  const SizedBox(height: 12),
                  FilledButton(
                    onPressed: () => ref.read(customersNotifierProvider.notifier).loadFirst(),
                    child: const Text('Reintentar'),
                  ),
                ],
              ),
            );
          }
          return RefreshIndicator(
            onRefresh: () => ref.read(customersNotifierProvider.notifier).refresh(),
            child: PaginatedListView(
              itemCount: items.length,
              hasMore: customersState.hasNext,
              onEndReached: () => ref.read(customersNotifierProvider.notifier).loadMore(),
              itemBuilder: (context, index) {
                final customer = items[index];
                final details = <Widget>[];
                if (customer.email != null && customer.email!.isNotEmpty) {
                  details.add(Text(customer.email!, style: Theme.of(context).textTheme.bodySmall));
                }
                if (customer.phone != null && customer.phone!.isNotEmpty) {
                  details.add(Text(customer.phone!, style: Theme.of(context).textTheme.bodySmall));
                }
                if (customer.lat != null || customer.lng != null) {
                  final lat = customer.lat != null ? customer.lat!.toStringAsFixed(6) : '-';
                  final lng = customer.lng != null ? customer.lng!.toStringAsFixed(6) : '-';
                  details.add(Text('Lat: $lat  Lng: $lng', style: Theme.of(context).textTheme.bodySmall));
                }
                return ListTile(
                  title: Text(customer.name),
                  subtitle: details.isEmpty
                      ? null
                      : Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          mainAxisSize: MainAxisSize.min,
                          children: details,
                        ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}
