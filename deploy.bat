@echo off

REM Nom de votre application
SET "CLASSES_DIR=bin"
SET "SRC_DIR=src\mg\itu\prom16\*.java"
SET "SRC_CLASSES=bin"


REM Compilation des classes Java
javac -d %CLASSES_DIR% -sourcepath src %SRC_DIR%

cd %CLASSES_DIR%
jar -cvf "..\jars\framework_2776.jar" *

if errorlevel 1 (
        pause
        exit /b 1
)