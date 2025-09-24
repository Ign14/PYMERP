class Page<T> {
  final List<T> items;
  final int pageNumber;
  final int pageSize;
  final bool hasNext;
  Page({required this.items, required this.pageNumber, required this.pageSize, required this.hasNext});
}
