import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';

class CustomerForm extends StatefulWidget {
  const CustomerForm({super.key, required this.onSubmit, this.initial});

  final void Function(Map<String, dynamic>) onSubmit;
  final Map<String, dynamic>? initial;

  @override
  State<CustomerForm> createState() => _CustomerFormState();
}

class _CustomerFormState extends State<CustomerForm> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _phone = TextEditingController();
  final _lat = TextEditingController();
  final _lng = TextEditingController();
  bool _locating = false;

  @override
  void initState() {
    super.initState();
    final data = widget.initial ?? const <String, dynamic>{};
    _name.text = data['name'] ?? '';
    _email.text = data['email'] ?? '';
    _phone.text = data['phone'] ?? '';
    _lat.text = _formatCoord(data['lat']);
    _lng.text = _formatCoord(data['lng']);
  }

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _phone.dispose();
    _lat.dispose();
    _lng.dispose();
    super.dispose();
  }

  String _formatCoord(Object? value) {
    if (value == null) return '';
    if (value is num) return value.toStringAsFixed(6);
    return value.toString();
  }

  double? _parseCoordinate(String value) {
    final trimmed = value.trim();
    if (trimmed.isEmpty) return null;
    final normalized = trimmed.replaceAll(',', '.');
    return double.tryParse(normalized);
  }

  Future<void> _fillCurrentLocation() async {
    if (_locating) return;
    setState(() => _locating = true);
    try {
      if (!await Geolocator.isLocationServiceEnabled()) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Servicio de ubicación deshabilitado')));
        return;
      }
      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.deniedForever || permission == LocationPermission.denied) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Permiso de ubicación denegado')));
        return;
      }
      final position = await Geolocator.getCurrentPosition(desiredAccuracy: LocationAccuracy.best);
      setState(() {
        _lat.text = position.latitude.toStringAsFixed(6);
        _lng.text = position.longitude.toStringAsFixed(6);
      });
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Ubicación actual aplicada')));
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('No se pudo obtener la ubicación')));
    } finally {
      if (mounted) {
        setState(() => _locating = false);
      }
    }
  }

  String? _validateLat(String? value) {
    final parsed = _parseCoordinate(value ?? '');
    if (parsed == null) return null;
    if (parsed < -90 || parsed > 90) {
      return 'Latitud inválida';
    }
    return null;
  }

  String? _validateLng(String? value) {
    final parsed = _parseCoordinate(value ?? '');
    if (parsed == null) return null;
    if (parsed < -180 || parsed > 180) {
      return 'Longitud inválida';
    }
    return null;
  }

  void _handleSubmit() {
    if (!_formKey.currentState!.validate()) return;
    final lat = _parseCoordinate(_lat.text);
    final lng = _parseCoordinate(_lng.text);
    widget.onSubmit({
      'name': _name.text.trim(),
      'email': _email.text.trim().isEmpty ? null : _email.text.trim(),
      'phone': _phone.text.trim().isEmpty ? null : _phone.text.trim(),
      'lat': lat,
      'lng': lng,
    });
  }

  @override
  Widget build(BuildContext context) {
    return Form(
      key: _formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          TextFormField(
            controller: _name,
            decoration: const InputDecoration(labelText: 'Nombre'),
            validator: (value) => (value == null || value.trim().isEmpty) ? 'Requerido' : null,
          ),
          TextFormField(
            controller: _email,
            decoration: const InputDecoration(labelText: 'Email'),
          ),
          TextFormField(
            controller: _phone,
            decoration: const InputDecoration(labelText: 'Teléfono'),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: TextFormField(
                  controller: _lat,
                  decoration: const InputDecoration(
                    labelText: 'Latitud (opcional)',
                    hintText: '-90.000000',
                    helperText: 'Rango permitido: -90..90',
                  ),
                  keyboardType: const TextInputType.numberWithOptions(decimal: true, signed: true),
                  validator: _validateLat,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextFormField(
                  controller: _lng,
                  decoration: const InputDecoration(
                    labelText: 'Longitud (opcional)',
                    hintText: '-180.000000',
                    helperText: 'Rango permitido: -180..180',
                  ),
                  keyboardType: const TextInputType.numberWithOptions(decimal: true, signed: true),
                  validator: _validateLng,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Align(
            alignment: Alignment.centerLeft,
            child: OutlinedButton.icon(
              onPressed: _locating ? null : _fillCurrentLocation,
              icon: _locating
                  ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                  : const Icon(Icons.my_location),
              label: const Text('Usar ubicación actual'),
            ),
          ),
          const SizedBox(height: 16),
          FilledButton(
            onPressed: _handleSubmit,
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
  }
}
