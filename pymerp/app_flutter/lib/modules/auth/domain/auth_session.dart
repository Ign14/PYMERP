import 'dart:convert';

class AuthSession {
  const AuthSession({
    required this.accessToken,
    required this.accessTokenExpiresAt,
    required this.refreshToken,
    required this.refreshTokenExpiresAt,
    required this.companyId,
    required this.email,
    required this.name,
  });

  final String accessToken;
  final DateTime accessTokenExpiresAt;
  final String refreshToken;
  final DateTime refreshTokenExpiresAt;
  final String companyId;
  final String email;
  final String name;

  bool get isAccessTokenExpired => DateTime.now().isAfter(accessTokenExpiresAt.subtract(const Duration(seconds: 5)));
  bool get isRefreshTokenExpired => DateTime.now().isAfter(refreshTokenExpiresAt.subtract(const Duration(seconds: 5)));

  Map<String, dynamic> toJson() => {
        'token': accessToken,
        'tokenExpiresAt': accessTokenExpiresAt.toIso8601String(),
        'refreshToken': refreshToken,
        'refreshTokenExpiresAt': refreshTokenExpiresAt.toIso8601String(),
        'companyId': companyId,
        'email': email,
        'name': name,
      };

  static AuthSession fromJson(Map<String, dynamic> json) {
    return AuthSession(
      accessToken: json['token'] as String,
      accessTokenExpiresAt: DateTime.parse(json['tokenExpiresAt'] as String),
      refreshToken: json['refreshToken'] as String,
      refreshTokenExpiresAt: DateTime.parse(json['refreshTokenExpiresAt'] as String),
      companyId: json['companyId'] as String,
      email: json['email'] as String,
      name: json['name'] as String,
    );
  }

  String toStorageString() => jsonEncode(toJson());

  static AuthSession? fromStorageString(String? raw) {
    if (raw == null) return null;
    return AuthSession.fromJson(jsonDecode(raw) as Map<String, dynamic>);
  }

  AuthSession copyWith({
    String? accessToken,
    DateTime? accessTokenExpiresAt,
    String? refreshToken,
    DateTime? refreshTokenExpiresAt,
    String? companyId,
    String? email,
    String? name,
  }) {
    return AuthSession(
      accessToken: accessToken ?? this.accessToken,
      accessTokenExpiresAt: accessTokenExpiresAt ?? this.accessTokenExpiresAt,
      refreshToken: refreshToken ?? this.refreshToken,
      refreshTokenExpiresAt: refreshTokenExpiresAt ?? this.refreshTokenExpiresAt,
      companyId: companyId ?? this.companyId,
      email: email ?? this.email,
      name: name ?? this.name,
    );
  }
}
