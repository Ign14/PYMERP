void logInfo(String message) {
  // placeholder: conecta con tu logger preferido
  // por ahora, imprime
  // ignore: avoid_print
  print('[INFO] $message');
}

void logError(Object error, [StackTrace? st]) {
  // ignore: avoid_print
  print('[ERROR] $error');
  if (st != null) {
    // ignore: avoid_print
    print(st);
  }
}
