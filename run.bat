@echo off
rem Builds and starts Park-O-Matic (Windows). Put the JDBC driver jar in the "lib" folder first.
cd /d "%~dp0"
call build.bat
java -cp "out;lib\*" com.parkomatic.Main
