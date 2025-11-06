@echo off
REM ===================================================================
REM EJECUTAR MORAPACK EN MODO LOCAL (H2 EN MEMORIA)
REM ===================================================================
REM Este script ejecuta MoraPack usando una base de datos H2 en memoria
REM NO necesitas instalar PostgreSQL ni ejecutar SQL
REM Hibernate crea todas las tablas automáticamente
REM ===================================================================

echo.
echo ========================================
echo   MORAPACK - MODO DESARROLLO LOCAL
echo ========================================
echo.
echo [INFO] Iniciando backend con H2 Database (en memoria)...
echo [INFO] Las tablas se crean automaticamente
echo [INFO] Consola H2: http://localhost:8080/h2-console
echo [INFO] Swagger: http://localhost:8080/swagger-ui.html
echo.

REM Ejecutar Spring Boot con perfil local
mvn spring-boot:run -Dspring-boot.run.profiles=local

pause

