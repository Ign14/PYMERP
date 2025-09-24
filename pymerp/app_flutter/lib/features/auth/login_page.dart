import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'auth_controller.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});
  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _controller = TextEditingController();
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Login (JWT token)')),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(children: [
          TextField(controller: _controller, decoration: const InputDecoration(labelText: 'JWT token')),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: () async {
              final token = _controller.text.trim();
              final state = ref.read(authTokenProvider.notifier);
              await AuthController(state).loginWithToken(token);
              if (context.mounted) Navigator.of(context).pushReplacementNamed('/customers');
            },
            child: const Text('Entrar'),
          )
        ]),
      ),
    );
  }
}
