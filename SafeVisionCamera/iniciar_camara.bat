@echo off
setlocal

cd /d "%~dp0"

set "PYTHON_EXE="

if exist "%LOCALAPPDATA%\Programs\Python\Python311\python.exe" (
    set "PYTHON_EXE=%LOCALAPPDATA%\Programs\Python\Python311\python.exe"
) else (
    where py >nul 2>&1
    if not errorlevel 1 (
        set "PYTHON_EXE=py -3.11"
    ) else (
        where python >nul 2>&1
        if not errorlevel 1 (
            set "PYTHON_EXE=python"
        )
    )
)

if "%PYTHON_EXE%"=="" (
    echo [ERROR] No se encontro Python 3.11 en el sistema.
    pause
    exit /b 1
)

echo Iniciando SafeVisionCamera con Python 3.11...
"%PYTHON_EXE%" servidor_camara.py

pause