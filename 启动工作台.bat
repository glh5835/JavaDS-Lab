@echo off

rem ============================================================

rem JavaDS-Lab 学习工作台 唯一启动入口（计划 §1.1 / §33）

rem 双击后：定位目录 -> 检查 JDK -> 检查构建产物 -> 启动服务 ->

rem         健康检查 -> 自动打开浏览器 -> 进入学习中心首页

rem ============================================================

setlocal EnableDelayedExpansion



rem 1) 以脚本自身位置定位项目目录，不依赖当前工作目录

set "ROOT=%~dp0"

set "ROOT_ARG=%ROOT:~0,-1%"

cd /d "%ROOT%"



rem 2) 检查 JDK 17（优先用项目内置便携版，其次系统 PATH）

set "JAVA_EXE="

if exist "%ROOT%tools\jdk-17.0.20.1+1\bin\java.exe" (

  set "JAVA_EXE=%ROOT%tools\jdk-17.0.20.1+1\bin\java.exe"

  set "JAVA_HOME=%ROOT%tools\jdk-17.0.20.1+1"

) else (

  where java >nul 2>nul && set "JAVA_EXE=java"

)

if not defined JAVA_EXE (

  echo [错误] 未检测到 JDK 17。

  echo 请安装 JDK 17，或将便携版放到 tools\jdk-17.0.20.1+1\ 目录。

  pause

  exit /b 1

)



rem 3) 检查服务端类是否已编译；缺失则用 Maven 编译

if not exist "%ROOT%target\classes\com\javadslab\workbench\WorkbenchServer.class" (

  echo 首次运行：正在编译项目（Maven，只需一次）……

  set "MAVEN_CMD=mvn"

  if exist "%ROOT%tools\apache-maven-3.9.16\bin\mvn.cmd" set "MAVEN_CMD=%ROOT%tools\apache-maven-3.9.16\bin\mvn.cmd"

  call "%MAVEN_CMD%" -s "%ROOT%tools\maven-settings.xml" -q compile

  if errorlevel 1 (

    echo [错误] 编译失败。请检查网络（Maven 首次需要下载依赖）后重试。

    pause

    exit /b 1

  )

)



rem 4) 组装 classpath（SQLite JDBC + slf4j）

set "USER_M2=%USERPROFILE%\.m2\repository"

set "CP=%ROOT%target\classes;%USER_M2%\org\xerial\sqlite-jdbc\3.46.1.0\sqlite-jdbc-3.46.1.0.jar;%USER_M2%\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar"

if not exist "%USER_M2%\org\xerial\sqlite-jdbc\3.46.1.0\sqlite-jdbc-3.46.1.0.jar" (

  echo [错误] 缺少 SQLite JDBC 依赖：%USER_M2%\org\xerial\sqlite-jdbc\3.46.1.0\sqlite-jdbc-3.46.1.0.jar

  echo 请先双击 运行测试.bat 一次，让 Maven 下载依赖。

  pause

  exit /b 1

)



rem 5) 检查端口占用（8642 被占用说明工作台可能已在运行，直接打开浏览器）

set "PORT=8642"

netstat -ano | findstr ":%PORT% " | findstr "LISTENING" >nul 2>nul

if not errorlevel 1 (

  echo 端口 %PORT% 已被占用：工作台可能已经在运行，直接打开浏览器。

  start "" "http://127.0.0.1:%PORT%/"

  pause

  exit /b 0

)



rem 6) 启动服务（前台运行，关闭本窗口即停止工作台）

echo 正在启动 JavaDS-Lab 学习工作台（端口 %PORT%）……

start "JavaDS-Lab 工作台" /min "%JAVA_EXE%" -Dfile.encoding=UTF-8 -cp "%CP%" com.javadslab.workbench.WorkbenchServer %PORT% "%ROOT_ARG%"



rem 7) 健康检查：最多等 30 秒，成功后自动打开浏览器（§34）

set /a TRIES=0

:waitloop

ping -n 2 127.0.0.1 >nul

curl -s -o nul http://127.0.0.1:%PORT%/api/health 2>nul

if not errorlevel 1 goto ready

set /a TRIES+=1

if %TRIES% lss 30 goto waitloop

echo [错误] 服务在 30 秒内未就绪。可能原因：

echo   - 端口被其他程序占用（编辑本脚本的 PORT 变量）

echo   - 防火墙拦截了本机端口

echo 详细日志：%ROOT%logs\workbench.log

pause

exit /b 1



:ready

echo 服务就绪，正在打开浏览器：http://127.0.0.1:%PORT%/

start "" "http://127.0.0.1:%PORT%/"

echo.

echo 工作台已在后台运行（关闭本窗口不会停止服务；

echo 停止服务请关闭任务栏中标题为 JavaDS-Lab 工作台 的最小化窗口）。

ping -n 5 127.0.0.1 >nul

exit /b 0

