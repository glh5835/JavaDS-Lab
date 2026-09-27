@echo off
rem JavaDS-Lab test runner: uses portable JDK/Maven from tools\ if present
cd /d "%~dp0"
if exist "tools\jdk-17.0.20.1+1\bin\java.exe" (
  set "JAVA_HOME=%~dp0tools\jdk-17.0.20.1+1"
  set "PATH=%~dp0tools\jdk-17.0.20.1+1\bin;%~dp0tools\apache-maven-3.9.16\bin;%PATH%"
)
mvn -s tools\maven-settings.xml test
pause
