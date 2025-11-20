@echo off
cd /d "%~dp0"
echo Starting backend with H2 profile...
echo.
gradlew.bat bootRun --args="--spring.profiles.active=h2" --no-daemon 2>&1
pause

