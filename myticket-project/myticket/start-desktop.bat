@echo off
set JAVA_HOME=C:\Users\sam\java21\jdk-21.0.10+7
set PATH=%JAVA_HOME%\bin;C:\Users\sam\maven\apache-maven-3.9.14\bin;%PATH%
cd /d "%~dp0"
echo Starting MyTicket desktop app...
mvn -pl myticket-desktop javafx:run
pause
