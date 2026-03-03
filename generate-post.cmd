@echo off
setlocal

@REM ============================================
@REM  Set your blog post topic here:
@REM ============================================
set "TOPIC=CRTP in C++: the curiously recurring template pattern and static polymorphism"
set "ITERATIONS=2"
set "IMAGES=true"
set "IMAGE_COUNT=5"
set "AUDIO=true"
@REM ============================================

@REM Load .env file
if exist "%~dp0.env" (
    for /f "usebackq tokens=1,* delims==" %%a in ("%~dp0.env") do (
        if not "%%a"=="" if not "%%b"=="" set "%%a=%%b"
    )
)

echo.
echo Starting server...
start /B "" mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local > nul 2>&1

echo Waiting for server to be ready...
:wait
timeout /t 2 /nobreak > nul
curl -s -o nul -w "" http://localhost:8080 2> nul
if %ERRORLEVEL% NEQ 0 goto wait

echo Server is ready. Generating blog post...
echo Topic: %TOPIC%
echo Images: %IMAGES% (max %IMAGE_COUNT%)
echo Audio: %AUDIO%
echo.

curl -s -X POST http://localhost:8080/api/generate -H "Content-Type: application/json" -d "{\"topic\": \"%TOPIC%\", \"iterations\": %ITERATIONS%, \"images\": %IMAGES%, \"imageCount\": %IMAGE_COUNT%, \"audio\": %AUDIO%}"

echo.
echo.
echo Shutting down server...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /T /F /PID %%a > nul 2>&1
)

echo Done.
endlocal
