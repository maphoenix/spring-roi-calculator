# Spring Boot ROI Calculator

A microservices-based ROI calculator with PDF and Excel export, Thymeleaf UI, and Swagger API.

## Project Structure

- `/api` - Spring Boot backend service
  - Source code, resources, and Maven configuration
  - REST API endpoints and business logic

## Requirements

- Java 17
- Maven 3.6+

## How to Build and Run

### Building the Application

```bash
# Clone the repository
git clone https://github.com/yourusername/spring-roi-calculator.git
cd spring-roi-calculator

# Build the API
cd api
mvn clean package
```

### Running the Application

```bash
# Run the API using Maven
cd api
mvn spring-boot:run

# Or run the JAR file directly
java -jar target/roi-calculator-1.0.0.jar
```

The application will start on http://localhost:8080

### Accessing the Application

- Web Interface: http://localhost:8080/roi/form
- Swagger API Documentation: http://localhost:8080/swagger-ui.html

## Features

- ROI calculation for smart tariffs
- PDF export of calculations
- Excel export of calculations
- Interactive web interface using Thymeleaf
- RESTful API documented with Swagger/OpenAPI
