import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/pagination.dart';
import '../../domain/customer.dart';
import '../customers/customers_controller.dart';
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
      ref.read(customersStateProvider.notifier).loadFirst();
    });
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(customersStateProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Clientes')),
      body: state.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Error: ')),
        data: (Page<Customer> page) {
          return RefreshIndicator(
            onRefresh: () => ref.read(customersStateProvider.notifier).loadFirst(),
            child: PaginatedListView(
              itemCount: page.items.length,
              hasMore: page.hasNext,
              onEndReached: () => ref.read(customersStateProvider.notifier).loadMore(),
              itemBuilder: (ctx, i) {
                final c = page.items[i];
                final details = <Widget>[];
                if (c.email != null) {
                  details.add(Text(c.email!, style: Theme.of(context).textTheme.bodySmall));
                }
                if (c.phone != null) {
                  details.add(Text(c.phone!, style: Theme.of(context).textTheme.bodySmall));
                }
                if (c.lat != null || c.lng != null) {
                  final latText = c.lat != null ? c.lat!.toStringAsFixed(6) : '—';
                  final lngText = c.lng != null ? c.lng!.toStringAsFixed(6) : '—';
                  details.add(Text('Lat:  · Lng: ', style: Theme.of(context).textTheme.bodySmall));
                }
                return ListTile(
                  title: Text(c.name),
                  subtitle: details.isEmpty ? null : Column(crossAxisAlignment: CrossAxisAlignment.start, children: details),
                );
              },
            ),
          );
        },
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.of(context).pushNamed('/customers/new');
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
