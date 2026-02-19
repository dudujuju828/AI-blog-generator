@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.3.2
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "WRAPPER_JAR=%MAVEN_USER_HOME%\wrapper\dists\maven-wrapper-3.3.2.jar"

@REM Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set "JAVACMD=java.exe"
%JAVACMD% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. >&2
goto error

:findJavaFromJavaHome
set "JAVACMD=%JAVA_HOME%\bin\java.exe"

if exist "%JAVACMD%" goto init

echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% >&2
goto error

:init

@REM Download wrapper jar if not present
if exist "%WRAPPER_JAR%" goto execute

set "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

for /f "tokens=1,* delims==" %%a in (%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties) do (
    if "%%a"=="wrapperUrl" set "WRAPPER_URL=%%b"
)

echo Downloading Maven Wrapper...

if not exist "%MAVEN_USER_HOME%\wrapper\dists" mkdir "%MAVEN_USER_HOME%\wrapper\dists"

powershell -Command "&{"^
    "$webclient = new-object System.Net.WebClient;"^
    "$webclient.DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"^
    "}"

if "%ERRORLEVEL%" NEQ "0" (
    del /f /q "%WRAPPER_JAR%" >NUL 2>&1
    echo ERROR: Failed to download Maven wrapper >&2
    goto error
)

:execute
"%JAVACMD%" ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath "%WRAPPER_JAR%" ^
  org.apache.maven.wrapper.MavenWrapperMain ^
  %MAVEN_CONFIG% %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
endlocal & set ERROR_CODE=%ERROR_CODE%

exit /b %ERROR_CODE%
