@echo off
chcp 65001 > nul
if not exist "bin\com\buu297\billing\Main.class" (
    echo Binaries not found. Running compiler first...
    call compile.bat
)
echo Starting Web Server for Student Payment Dashboard at http://localhost:8080...
start "" "http://localhost:8080"
java -cp "bin;lib\*;src\main\resources" com.buu297.billing.Main --server
