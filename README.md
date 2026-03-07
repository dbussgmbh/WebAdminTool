# Vaadin Oracle User Management SQL

Diese Version verwendet kein Flyway und kein JPA.
Der DB-Zugriff läuft über `NamedParameterJdbcTemplate` und echte SQL-Abfragen.

Vor dem Start bitte `sql/init.sql` in Oracle ausführen.

Start:
```bash
docker compose up -d
mvn clean spring-boot:run
```
