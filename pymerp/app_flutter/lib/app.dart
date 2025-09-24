import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core/config.dart';
import 'core/api_client.dart';
import 'features/auth/login_page.dart';
import 'features/customers/customers_controller.dart';
import 'features/customers/customers_page.dart';
import 'features/customers/customer_form_page.dart';
import 'features/settings/settings_page.dart';
import 'package:go_router/go_router.dart';

class App extends ConsumerStatefulWidget {
  const App({super.key});
  @override
  ConsumerState<App> createState() => _AppState();
}

class _AppState extends ConsumerState<App> {
  late final AppConfig _cfg;
  late final ApiClient _api;
  late final GoRouter _router;

  @override
  void initState() {
    super.initState();
    _cfg = AppConfig.fromEnv();
    _api = ApiClient(_cfg);
    _router = GoRouter(
      initialLocation: '/login',
      routes: [
        GoRoute(path: '/login', builder: (c, s) => const LoginPage()),
        GoRoute(path: '/customers', builder: (c, s) => const CustomersPage()),
        GoRoute(path: '/customers/new', builder: (c, s) => const CustomerFormPage()),
        GoRoute(path: '/settings', builder: (c, s) => const SettingsPage()),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return ProviderScope(
      overrides: [
        customerServiceProvider.overrideWithValue(CustomerService(_api)),
      ],
      child: MaterialApp.router(title: 'PYMERP', theme: ThemeData(useMaterial3: true), routerConfig: _router),
    );
  }
}
