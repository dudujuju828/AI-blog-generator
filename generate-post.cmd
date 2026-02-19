@echo off
setlocal

@REM ============================================
@REM  Set your blog post topic here:
@REM ============================================
set "TOPIC=The history of polymorphism"
set "ITERATIONS=2"
@REM ============================================

echo.
echo Starting server...
start /B "" cmd /C "mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"" > nul 2>&1

echo Waiting for server to be ready...
:wait
timeout /t 2 /nobreak > nul
curl -s -o nul -w "" http://localhost:8080 2> nul
if %ERRORLEVEL% NEQ 0 goto wait

echo Server is ready. Generating blog post...
echo Topic: %TOPIC%
echo.

curl -s -X POST http://localhost:8080/api/generate -H "Content-Type: application/json" -d "{\"topic\": \"%TOPIC%\", \"iterations\": %ITERATIONS%}"

echo.
echo.
echo Shutting down server...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /PID %%a /F > nul 2>&1
)

echo Done.
endlocal
