#!/bin/bash

# Start Redis
redis-server --daemonize yes

# Start the Spring Boot application
cd /app
exec mvn spring-boot:run -Dspring-boot.run.profiles=dev -DskipTests -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=false"
