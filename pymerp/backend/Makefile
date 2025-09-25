.PHONY: bootstrap lint format test

bootstrap: ## Prepare local development environment
	./gradlew clean build

lint: ## Run static analysis (Checkstyle + Spotless)
	./gradlew checkstyleMain checkstyleTest spotlessCheck

format: ## Format source code using Spotless
	./gradlew spotlessApply

test: ## Execute backend test suite
	./gradlew test
