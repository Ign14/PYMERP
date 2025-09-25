import 'package:flutter/material.dart';

class PaginatedListView extends StatefulWidget {
  const PaginatedListView({super.key, required this.itemCount, required this.itemBuilder, required this.onEndReached, this.hasMore = false});
  final int itemCount;
  final IndexedWidgetBuilder itemBuilder;
  final VoidCallback onEndReached;
  final bool hasMore;

  @override
  State<PaginatedListView> createState() => _PaginatedListViewState();
}

class _PaginatedListViewState extends State<PaginatedListView> {
  final _controller = ScrollController();

  @override
  void initState() {
    super.initState();
    _controller.addListener(() {
      if (_controller.position.pixels >= _controller.position.maxScrollExtent - 200) {
        if (widget.hasMore) widget.onEndReached();
      }
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      controller: _controller,
      physics: const AlwaysScrollableScrollPhysics(),
      itemCount: widget.itemCount + (widget.hasMore ? 1 : 0),
      itemBuilder: (ctx, i) {
        if (widget.hasMore && i == widget.itemCount) {
          return const Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator()));
        }
        return widget.itemBuilder(ctx, i);
      },
    );
  }
}
