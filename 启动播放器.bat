@echo off
rem JavaDS-Lab 可视化播放器启动器：双击即用浏览器打开（trace 已内嵌，无需服务器）
start "" "%~dp0web\player.html"
echo 已在默认浏览器打开播放器。若页面空白，请改用 docs\使用说明.md 里的 http.server 方式。
