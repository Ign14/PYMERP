# Development Seed Data

This directory contains Flyway migration files that should **ONLY** be executed in development environments.

## Files

- **V3__seed_dev.sql**: Initial development seed data (test companies, users, products)
- **V9__seed_demo_data.sql**: Additional demo data for UI testing

## Configuration

### Development Profile
In `application-dev.yml`, these migrations are included:
```yaml
flyway:
  locations: classpath:db/migration,classpath:db/migration/dev
```

### Production Profile
In `application-prod.yml`, these files are **excluded** via pattern matching:
```yaml
flyway:
  locations: classpath:db/migration
  ignore-migration-patterns: "*:ignored,*:future,*seed_dev*,*seed_demo*"
```

## Security Note

⚠️ **NEVER** run these seed files in production. They contain:
- Weak passwords (`password123`)
- Test credit card numbers
- Fake customer data
- Demo billing information

These files are intentionally separated to prevent accidental execution in production environments.
