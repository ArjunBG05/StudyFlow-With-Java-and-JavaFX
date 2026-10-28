@echo off
REM Usage: run.bat           interactive menu
REM        run.bat --demo    non-interactive tour of every feature
if not exist out call build.bat
java -cp out com.studyflow.Main %*
