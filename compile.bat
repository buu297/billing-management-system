@echo off
chcp 65001 > nul
echo ===================================================
echo   Compiling Student Billing Management System
echo ===================================================
if not exist "bin" mkdir bin

rem Include lib/*.jar in classpath for MySQL driver
set CP=lib\*;src\main\resources

dir /s /B src\main\java\*.java > sources.txt
javac -encoding UTF-8 -cp "%CP%" -d bin @sources.txt
del sources.txt

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Compilation completed with 0 errors! Classes generated in /bin
) else (
    echo.
    echo [ERROR] Compilation failed!
)
