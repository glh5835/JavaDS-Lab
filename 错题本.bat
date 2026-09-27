@echo off
rem JavaDS-Lab Mistake Book CLI: double-click to start (data: data\lab.db)
cd /d "%~dp0"
if exist "tools\jdk-17.0.20.1+1\bin\java.exe" (
  set "JAVA_HOME=%~dp0tools\jdk-17.0.20.1+1"
  set "PATH=%~dp0tools\jdk-17.0.20.1+1\bin;%PATH%"
)
if not exist "target\classes" (
  echo Not compiled yet. Please run 运行测试.bat first.
  pause
  exit /b 1
)
set "J1=%USERPROFILE%\.m2\repository\org\xerial\sqlite-jdbc\3.46.1.0\sqlite-jdbc-3.46.1.0.jar"
set "J2=%USERPROFILE%\.m2\repository\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar"
if not exist "%J1%" (
  echo Missing dependency: %J1%
  echo Please run 运行测试.bat once so Maven downloads dependencies.
  pause
  exit /b 1
)
java -Dfile.encoding=UTF-8 -cp "target\classes;%J1%;%J2%" com.javadslab.app.MistakeBookCli data\lab.db
pause
