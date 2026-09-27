@echo off
rem JavaDS-Lab player launcher: opens web\player.html in the default browser
rem (all 44 traces are embedded in web\traces-bundle.js, no server needed)
start "" "%~dp0web\player.html"
echo Player opened in your default browser.
echo If the page is blank, see docs\使用说明.md for the http.server method.
