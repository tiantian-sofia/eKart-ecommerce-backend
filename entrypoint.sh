#!/bin/bash

# Start PostgreSQL
service postgresql start

# Start Redis
redis-server --daemonize yes

# Start the Spring Boot application
cd /app
exec mvn spring-boot:run -Dspring-boot.run.profiles=docker -DskipTests
