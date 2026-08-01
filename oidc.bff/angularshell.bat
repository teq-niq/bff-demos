@echo off

REM Absolute path to project
set PROJECT_DIR=%~dp0
set PROJECT_DIR=%PROJECT_DIR:~0,-1%

REM Force angular-front-end root as working directory
pushd %PROJECT_DIR%\angular-front-end

REM Isolated PATH (does NOT include existing PATH)
set PATH=%PROJECT_DIR%\..\node;%PROJECT_DIR%\angular-front-end\node_modules\.bin

echo Using isolated Node environment:
call node -v
call npm -v
call ng version

REM Start  cmd shell in project root
cmd /K
