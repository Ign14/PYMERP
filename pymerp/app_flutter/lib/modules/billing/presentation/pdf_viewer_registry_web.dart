import 'dart:ui' as ui;

typedef ViewFactory = dynamic Function(int viewId);

void registerPdfViewFactory(String viewType, ViewFactory factory) {
  // ignore: undefined_prefixed_name
  ui.platformViewRegistry.registerViewFactory(viewType, factory);
}
