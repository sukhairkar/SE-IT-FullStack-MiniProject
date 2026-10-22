@echo off
title Bookstore Management System
echo ===============================================================================
echo Starting Bookstore Management System (Javalin + Hibernate + MySQL)
echo ===============================================================================

cd /d "%~dp0"
call mvn clean compile exec:java

pause
