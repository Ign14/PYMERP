import 'pdf_viewer_registry_stub.dart'
    if (dart.library.html) 'pdf_viewer_registry_web.dart' as impl;

typedef ViewFactory = dynamic Function(int viewId);

void registerPdfViewFactory(String viewType, ViewFactory factory) {
  impl.registerPdfViewFactory(viewType, factory);
}
