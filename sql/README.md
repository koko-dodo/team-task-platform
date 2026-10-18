# Database setup

For a **new, empty** MySQL 8.0.16+ database only:

```sh
mysql -u root -p < sql/01_schema.sql
mysql -u root -p < sql/02_constraints.sql
```

Create a dedicated local application account with access to `group15_lab` using your
own credentials. No sample accounts, password hashes, or course datasets are shipped.
Register accounts through the application.

The schema includes team status, former assignee metadata and task dependencies.
Task/team dates match `LocalDate`; other timestamps match `LocalDateTime`.
Identifiers use signed BIGINT to match Java Long. Log team IDs are historical scalar
values, not foreign-key associations. These scripts are not an existing-data migration.
Back up and review any migration separately; do not run these on an existing database.
The original application's `ddl-auto=update` remains for local course-project use.
Use `SPRING_JPA_HIBERNATE_DDL_AUTO=validate` to check a freshly initialized schema.
