import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'config.dart';

final appConfigProvider = Provider<AppConfig>((ref) {
  return AppConfig.fromEnv();
});
