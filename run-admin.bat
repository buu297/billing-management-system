@echo off
chcp 65001 > nul
if not exist "bin\com\buu297\billing\Main.class" (
    echo Binaries not found. Running compiler first...
    call compile.bat
)
echo Starting Java Swing Admin Console & Report Generator...
java -cp "bin;lib\*;src\main\resources" com.buu297.billing.Main --admin
