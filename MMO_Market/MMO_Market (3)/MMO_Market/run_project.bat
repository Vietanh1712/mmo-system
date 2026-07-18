@echo off
color 0A
echo ==========================================
echo KHOI CHAY DU AN MMO MARKET SYSTEM
echo ==========================================

echo Dang khoi dong MMO Market Backend (Spring Boot)...
cd apps\backend
"C:\Users\pc\Downloads\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin\mvn.cmd" spring-boot:run "-Dmaven.test.skip=true"
