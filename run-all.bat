@echo off
chcp 65001 > nul
if not exist "bin\com\buu297\billing\Main.class" (
    echo Binaries not found. Running compiler first...
    call compile.bat
)
echo Starting Student Billing Management System (Dual Mode: Web Server + Swing Admin)...
start "" "http://localhost:8080"
java -cp "bin;lib\*;src\main\resources" com.buu297.billing.Main
