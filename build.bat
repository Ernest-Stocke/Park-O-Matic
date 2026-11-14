@echo off
rem Compiles the application into the "out" folder (Windows).
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\main\*.java > sources.txt
javac -d out @sources.txt
del sources.txt
echo Build finished (see messages above for any errors).
