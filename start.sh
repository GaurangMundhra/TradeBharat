#!/bin/bash

echo ""
echo "========================================"
echo "FinSimX Trading Engine - Backend Setup"
echo "========================================"
echo ""

echo "[1/3] Starting PostgreSQL container..."
docker-compose up -d postgres

echo ""
echo "[2/3] Waiting for PostgreSQL to be ready..."
sleep 10

echo ""
echo "[3/3] Building and running Spring Boot application..."
mvn clean spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

echo ""
echo "========================================"
echo "Backend running at http://localhost:8080"
echo "========================================"
