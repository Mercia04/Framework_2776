@echo off

REM Nom de votre application
SET "CLASSES_DIR=bin"
SET "SRC_DIR=src\mg\itu\prom16\*.java"
SET "SRC_CLASSES=bin"
SET "Lib_dir=c:\Work\sprint\Test\lib"


REM Compilation des classes Java
javac -parameters -d %CLASSES_DIR% -sourcepath src %SRC_DIR% -cp %Lib_dir%

cd %CLASSES_DIR%
jar -cvf "..\jars\framework_2776.jar" *

if errorlevel 1 (
        pause
        exit /b 1
)