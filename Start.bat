@echo off
rem mvn clean package -Pproduction

echo Lade .env Konfiguration...

REM .env Datei einlesen
for /f "usebackq tokens=1,* delims==" %%A in ("C:\Users\mquas\Downloads\vaadin-oracle-user-management-sql\src\main\resources\.env") do (
    set %%A=%%B
)

echo Starte Application...

C:\Java\jdk22\bin\java -Xms512m -Xmx2g -jar target\vaadin-oracle-user-management-sql-0.0.1-SNAPSHOT.jar

pause