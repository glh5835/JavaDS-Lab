@echo off
chcp 65001 >nul
rem 旧播放器入口已按计划（§1.1）迁移：不再单独打开 web\player.html，
rem 统一转发到新的学习工作台（其中已完整整合 SVG 播放器与全部 44 个 Trace）。
echo [提示] 独立播放器已整合进学习工作台。
echo 正在打开工作台（含完整播放器：单步/回退/播放/变速/进度/缩略图/JSON 导入导出）……
call "%~dp0启动工作台.bat"
