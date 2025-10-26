typedef ViewFactory = dynamic Function(int viewId);

void registerPdfViewFactory(String viewType, ViewFactory factory) {}
