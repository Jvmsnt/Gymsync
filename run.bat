@echo off
cd /d "%~dp0"
javac -cp ".;lib/*" GymsyncServer.java gymsyncapp.java
if %errorlevel% neq 0 (
    echo Build failed.
    pause
    exit /b %errorlevel%
)
java -cp ".;lib/*" GymsyncServer
