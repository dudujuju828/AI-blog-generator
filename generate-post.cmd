@echo off
setlocal enabledelayedexpansion

@REM ============================================
@REM  Set your blog post topic here:
@REM ============================================
set "TOPIC=Leukocytes: what they are, how they work, and their importance in pharmacy"
set "ITERATIONS=2"
set "IMAGES=true"
set "IMAGE_COUNT=7"
set "AUDIO=true"
@REM Optional: path to a file with one resource URL or snippet per line
set "RESOURCES_FILE="
@REM ============================================

@REM Load .env file
if exist "%~dp0.env" (
    for /f "usebackq tokens=1,* delims==" %%a in ("%~dp0.env") do (
        if not "%%a"=="" if not "%%b"=="" set "%%a=%%b"
    )
)

@REM Build resources JSON array from file
set "RESOURCES_JSON="
if defined RESOURCES_FILE (
    if exist "%RESOURCES_FILE%" (
        set "RESOURCES_JSON=, \"resources\": ["
        set "FIRST=1"
        for /f "usebackq delims=" %%r in ("%RESOURCES_FILE%") do (
            if !FIRST!==1 (
                set "RESOURCES_JSON=!RESOURCES_JSON!\"%%r\""
                set "FIRST=0"
            ) else (
                set "RESOURCES_JSON=!RESOURCES_JSON!, \"%%r\""
            )
        )
        set "RESOURCES_JSON=!RESOURCES_JSON!]"
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
if defined RESOURCES_FILE (
    if exist "%RESOURCES_FILE%" echo Resources: %RESOURCES_FILE%
)
echo.

curl -s -X POST http://localhost:8080/api/generate -H "Content-Type: application/json" -d "{\"topic\": \"%TOPIC%\", \"iterations\": %ITERATIONS%, \"images\": %IMAGES%, \"imageCount\": %IMAGE_COUNT%, \"audio\": %AUDIO%!RESOURCES_JSON!}"

echo.
echo.
echo Shutting down server...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING') do (
    taskkill /T /F /PID %%a > nul 2>&1
)

echo Done.
endlocal
