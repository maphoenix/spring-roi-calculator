# Spring Boot ROI Calculator

A microservices-based ROI calculator for solar and battery installations with battery degradation modeling, time series data, PDF and Excel export, Thymeleaf UI, and REST API.

## Tasks:

## Front-end:

1. Have a form page for the default inputs
2. Dashboard page for sending an API request to the spring service and tweakable inputs like the resources/templates/results.html

- Add a disclaimer as this is a guide only for approximate estimations and we use smart defaults, legal disclaimer.

## Back-end:

2. Only return the Tarrifs that are relevant given the inputs for a user. I.e. if no Ev, exclude Intelligent Octopus Go
3. ROI Service: Enrich the time series data, so that you have yearly plots
   2.1 Double check the ROI service, because if you have no EV tariff but if you have only battery, this should be a loss leader.
   2.2 Change the RoiService so that you can say with a piece of text at the top + visualisations, what the breakeven year is. And how much profit they will make before the batteries + solar need replacing.
   2.3 Is there something to penalise not having solar because you get the same output but it's inflation proof?
   2.4 Is there something in tax releief, as the user would not have to pay taxable money in the future? But technically they stil have to get taxed to buy the solar? Or taxed if they are given the money?
   2.4 Borrowing vs cash buy for solar + batteries?
4. Lost opportunity cost, if they had invested the money in an SPY index at an assumed growth rate what would they have made? Please take inflation into account.
5. http://localhost:8080/documentation clicking this should load target="\_blank" as an <a> text link rather than redirecting

## Project Structure

- `/api` - Spring Boot backend service
  - Source code, resources, and Maven configuration
  - REST API endpoints and business logic
  - Battery degradation modeling and ROI time series calculation

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

**1. Run the Backend API**

Open a terminal and navigate to the `api` directory:

```bash
# Navigate to the API directory
cd api

# Run the API using Maven
mvn spring-boot:run

# Or run the JAR file directly if already built
# java -jar target/roi-calculator-1.0.0.jar
```

The backend API will start on http://localhost:8080.

**2. Run the Frontend Client**

Open a _separate_ terminal and navigate to the `client` directory:

```bash
# Navigate to the Client directory from the project root
cd ../client
# Or if you are still in the api directory: cd ../client

# Install dependencies (if you haven't already)
npm install
# or yarn install

# Start the client development server
npm run dev
# or yarn dev
```

The frontend client will typically start on http://localhost:3000 (check the terminal output for the exact URL).

### Accessing the Application

- Web Interface: http://localhost:8080/roi/form
- User Profile Form: http://localhost:8080/
- API Documentation: http://localhost:8080/swagger-ui.html
- Time Series Data API: http://localhost:8080/api/roi/timeseries

## Features

### Core ROI Calculation

- ROI calculation for smart tariffs with different peak, off-peak, and export rates
- Battery and solar system sizing based on household profiles
- Adjusts for EV ownership and home occupancy patterns

### Battery Degradation Modeling

- Realistic battery degradation modeling (70% capacity after 10 years)
- Maximum battery lifespan of 15 years
- Year-by-year calculation of effective battery capacity

### Time Series Analysis

- Annual savings calculation over the system lifetime
- Cumulative ROI calculation with initial system costs
- Payback period determination for different tariffs
- Year-by-year visualization data for financial projections

### User Interface

- Interactive web interface using Thymeleaf
- User profile customization
- ROI calculation dashboard with different tariff comparisons

### API Endpoints

- RESTful API for ROI calculations
- Time series data for visualization through dedicated endpoints
- Support for both GET and POST requests with parameter customization

## API Usage Examples

### Fetching Time Series Data (GET)

```bash
# Default parameters
curl http://localhost:8080/api/roi/timeseries

# Custom parameters
curl "http://localhost:8080/api/roi/timeseries?batterySize=20&solarSize=6&usage=5000"
```

### Calculating ROI with Custom Parameters (POST)

```bash
curl -X POST http://localhost:8080/api/roi/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "batterySize": 15.0,
    "usage": 4500,
    "solarSize": 5.0
  }'
```

## Battery Degradation Model

The application models battery degradation with:

- Linear decline to 70% capacity by year 10
- Accelerated decline from year 10 to 15
- Year 15 as end-of-life for battery calculations

This provides realistic ROI projections that account for the decreasing efficiency of battery systems over time.
