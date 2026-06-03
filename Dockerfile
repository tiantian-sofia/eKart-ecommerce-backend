FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    openjdk-17-jdk maven git postgresql redis-server \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN git clone https://github.com/tiantian-sofia/eKart-ecommerce-backend.git .

RUN mvn dependency:go-offline -DskipTests

# Initialize PostgreSQL database
RUN service postgresql start && \
    su postgres -c "psql -c \"CREATE USER ekart WITH PASSWORD 'ekart123';\"" && \
    su postgres -c "psql -c \"CREATE DATABASE ekart OWNER ekart;\"" && \
    service postgresql stop

# Set environment variables for the app
ENV dbUrl=jdbc:postgresql://localhost:5432/ekart \
    dbUsername=ekart \
    dbPassword=ekart123 \
    REDIS_HOST=localhost \
    SPRING_PROFILES_ACTIVE=docker \
    jsonSecretKey=ekartSecretKey \
    adminPassword=Admin@123 \
    stripeApiKey=sk_test_dummy \
    stripeEndpointSecret=dummy \
    emailUsername=dummy@gmail.com \
    emailPassword=dummy

EXPOSE 8000

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

CMD ["/entrypoint.sh"]
