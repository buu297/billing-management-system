@echo off
chcp 65001 > nul
echo ===================================================
echo   Setting up MySQL Database: billing_db
echo ===================================================
set /p DB_USER="Enter MySQL Username [root]: "
if "%DB_USER%"=="" set DB_USER=root
set /p DB_PASS="Enter MySQL Password: "

echo.
echo Executing schema.sql...
mysql -u %DB_USER% -p%DB_PASS% < "%~dp0schema.sql"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to execute schema.sql. Please check your credentials.
    pause
    exit /b %ERRORLEVEL%
)

echo Executing seed_data.sql...
mysql -u %DB_USER% -p%DB_PASS% < "%~dp0seed_data.sql"
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Failed to execute seed_data.sql.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [SUCCESS] Database billing_db setup complete!
pause
