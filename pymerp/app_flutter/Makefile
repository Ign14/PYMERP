.PHONY: bootstrap gen analyze test run-web run-win run-android

bootstrap:
	flutter create --platforms=web,windows,android .
	flutter pub get

gen:
	flutter pub run build_runner build --delete-conflicting-outputs

analyze:
	flutter analyze

test:
	flutter test

run-web:
	flutter run -d chrome --dart-define=BASE_URL=http://localhost:8081 --dart-define=COMPANY_ID=dev-company

run-win:
	flutter run -d windows --dart-define=BASE_URL=http://localhost:8081 --dart-define=COMPANY_ID=dev-company

run-android:
	flutter run -d android --dart-define=BASE_URL=http://10.0.2.2:8081 --dart-define=COMPANY_ID=dev-company
