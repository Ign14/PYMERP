import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pymerp_app/modules/customers/presentation/customer_form.dart';

void main() {
  testWidgets('CustomerForm renders optional geo fields and validates range', (tester) async {
    Map<String, dynamic>? submitted;
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: CustomerForm(
          onSubmit: (data) => submitted = data,
        ),
      ),
    ));

    expect(find.byType(TextFormField), findsNWidgets(5));
    await tester.enterText(find.widgetWithText(TextFormField, 'Latitud (opcional)'), '100');
    await tester.tap(find.text('Guardar'));
    await tester.pump();
    expect(find.text('Latitud inválida'), findsOneWidget);

    await tester.enterText(find.widgetWithText(TextFormField, 'Latitud (opcional)'), '-33,456789');
    await tester.enterText(find.widgetWithText(TextFormField, 'Longitud (opcional)'), '-70.123456');
    await tester.enterText(find.widgetWithText(TextFormField, 'Nombre'), 'Cliente Test');
    await tester.tap(find.text('Guardar'));
    await tester.pump();

    expect(submitted, isNotNull);
    expect(submitted!['lat'], closeTo(-33.456789, 1e-6));
    expect(submitted!['lng'], closeTo(-70.123456, 1e-6));
  });
}
