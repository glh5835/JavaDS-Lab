@echo off
rem JavaDS-Lab 一键测试：优先用便携 JDK/Maven（tools/），没有则用系统 PATH
cd /d "%~dp0"
if exist "tools\jdk-17.0.20.1+1\bin\java.exe" (
  set "JAVA_HOME=%~dp0tools\jdk-17.0.20.1+1"
  set "PATH=%JAVA_HOME%\bin;%~dp0tools\apache-maven-3.9.16\bin;%PATH%"
)
mvn -s tools\maven-settings.xml test
pause
