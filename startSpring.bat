@echo off
title run Set debug and run ajsc for aai module
CALL set-debug-port.bat
START mvn spring-boot:run