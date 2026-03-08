# Vaadin Oracle User Management SQL

Diese Version verwendet kein Flyway und kein JPA.
Der DB-Zugriff läuft über `NamedParameterJdbcTemplate` und echte SQL-Abfragen.

Vor dem Start bitte `sql/init.sql` in Oracle ausführen.

mvn clean package -Pproduction
C:\Java\jdk22\bin\java -jar target/vaadin-oracle-user-management-sql-0.0.1-SNAPSHOT.jar
