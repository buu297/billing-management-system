@echo off
chcp 65001 > nul
if not exist "bin\com\buu297\billing\SystemVerificationTest.class" (
    echo Binaries not found. Running compiler first...
    call compile.bat
)
echo Running System Verification and ACID Concurrency Test Suite...
java -ea -cp "bin;lib\*;src\main\resources" com.buu297.billing.SystemVerificationTest