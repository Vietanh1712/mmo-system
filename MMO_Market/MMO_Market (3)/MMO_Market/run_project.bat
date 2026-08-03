@echo off
color 0A
echo ==========================================
echo KHOI CHAY DU AN MMO MARKET SYSTEM
echo ==========================================

echo Dang khoi dong MMO Market Backend (Spring Boot)...
cd apps\backend
mvn spring-boot:run "-Dmaven.test.skip=true"
