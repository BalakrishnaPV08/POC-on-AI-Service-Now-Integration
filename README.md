# POC on AI – ServiceNow Integration

## Project Overview

This project is the foundation application for a 3-step Application Operations / AI Agent vision.

### Goal – 3-Step Vision

1. **Step 1 – Foundation Application**
    - Build a sample Spring Boot application.
    - Application interacts with PostgreSQL.
    - Application uses RabbitMQ for asynchronous messaging.
    - Application contains controlled error scenarios for observability testing.

2. **Step 2 – Dynatrace Integration**
    - Dynatrace Agent detects application errors and abnormal behavior.
    - Errors and performance issues are exposed through application logs and metrics.
    - Detected problems can be used to automatically create ServiceNow incidents.

3. **Step 3 – AI Agent / Multi-Agent Integration**
    - AI Agent sends notifications.
    - AI Agent coordinates between Dynatrace, ServiceNow and other agents.
    - AI Agent can help analyze application incidents and coordinate remediation.

> **This repository implements Step 1 – the foundation application.**

---

# Architecture

```text
                         +----------------------+
                         |        Client        |
                         +----------+-----------+
                                    |
                         REST       |
                    +---------------+---------------+
                    |                               |
                    v                               v
          +-----------------+             +-------------------------+
          | OrderController |             | ErrorSimulatorController|
          +--------+--------+             +-----------+-------------+
                   |                                  |
                   +---------------+------------------+
                                   |
                                   v
                          +-----------------+
                          |   OrderService  |
                          +--------+--------+
                                   |
                    +--------------+--------------+
                    |                             |
                  save                         publish
                    |                             |
                    v                             v
          +------------------+          +------------------+
          |   PostgreSQL DB  |          |     RabbitMQ     |
          |     / Orders     |          |   order queue   |
          +------------------+          +--------+---------+
                                                |
                                              consume
                                                |
                                                v
                                      +------------------+
                                      |  OrderConsumer   |
                                      +--------+---------+
                                               |
                                          update status
                                               |
                                               v
                                      +------------------+
                                      |   PostgreSQL DB  |
                                      +------------------+

       JSON Logs
Spring Boot -------------------------> Dynatrace Agent

       Actuator / Prometheus
Spring Boot -------------------------> Prometheus
```

---

# Technology Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Language | Java 17 |
| Database | PostgreSQL |
| Database Access | Spring Data JPA |
| Messaging | RabbitMQ / AMQP |
| Monitoring | Spring Boot Actuator |
| Metrics | Micrometer + Prometheus |
| Logging | Logback |
| Log Format | JSON |
| Dynatrace Integration | JSON logs / Logstash encoder |
| Infrastructure | Docker Compose |

---

# Key Components

| Component | Purpose |
|---|---|
| `OrderController` | REST API for submitting and retrieving orders |
| `ErrorSimulatorController` | REST APIs used to deliberately trigger controlled application failures |
| `OrderService` | Core order-processing and business logic |
| `OrderProducer` | Publishes order messages to RabbitMQ |
| `OrderConsumer` | Asynchronously consumes RabbitMQ messages and updates order status |
| `AuditLog` | Records application actions such as submit, consume and error events |

---

# Application Flow

## Normal Order Flow

```text
Client
   |
   | POST /api/orders
   v
OrderController
   |
   v
OrderService
   |
   +---------------> PostgreSQL
   |                     |
   |                     +-- Save Order
   |
   +---------------> RabbitMQ
                         |
                         v
                   OrderConsumer
                         |
                         +-- Update Order Status
```

The application provides both:

- **Synchronous database interaction**
- **Asynchronous RabbitMQ processing**

This allows Dynatrace to observe database failures, application exceptions, messaging failures and slow processing.

---

# Error Simulation

`ErrorSimulatorController` is used to deliberately generate different failure conditions.

These controlled failures are required so that Dynatrace can detect different types of application problems.

## 10 Error Scenarios for Dynatrace

| # | Scenario | Trigger |
|---|---|---|
| 1 | DB Duplicate Key | `POST /api/errors/duplicate-order` |
| 2 | DB Null Constraint | `POST /api/orders` with empty body |
| 3 | Slow Query | `POST /api/errors/slow-query` |
| 4 | NullPointerException | `POST /api/errors/npe` |
| 5 | Optimistic Lock Conflict | `POST /api/errors/optimistic-lock/{id}` |
| 6 | Poison RabbitMQ Message | `POST /api/errors/poison-message` |
| 7 | Consumer Crash | `POST /api/errors/consumer-crash` |
| 8 | High-Value Business Alert | Order with total greater than `$10,000` |
| 9 | Slow Message Processing | Consumer processing delay |
| 10 | DB Connection Pool Exhausted | `POST /api/errors/db-connection` |

---

# Error Scenario Details

## 1. DB Duplicate Key

Attempts to create an order using an existing identifier.

```http
POST /api/errors/duplicate-order
```

Expected result:

- Database duplicate-key violation.
- Spring application logs the exception.
- Dynatrace detects the database/application error.

---

## 2. DB Null Constraint

Submits an invalid order with required fields missing.

```http
POST /api/orders
```

Example:

```json
{}
```

Expected result:

- Database constraint violation.
- Application error is logged.
- Dynatrace can detect the failure.

---

## 3. Slow Query

Triggers a deliberately slow database operation.

```http
POST /api/errors/slow-query
```

Expected result:

- Increased database response time.
- Slow request visible in Dynatrace.
- Useful for demonstrating performance monitoring.

---

## 4. Null Pointer Exception

Triggers an application-level `NullPointerException`.

```http
POST /api/errors/npe
```

Expected result:

- Application exception.
- Stack trace written to JSON logs.
- Dynatrace detects the application error.

---

## 5. Optimistic Lock Conflict

Simulates concurrent modification of an order.

```http
POST /api/errors/optimistic-lock/{id}
```

Example:

```http
POST /api/errors/optimistic-lock/1
```

Expected result:

- Optimistic locking conflict.
- Transaction/application error.
- Error becomes visible to Dynatrace.

---

## 6. Poison RabbitMQ Message

Publishes a message that cannot be processed successfully by the consumer.

```http
POST /api/errors/poison-message
```

Expected flow:

```text
Producer
   |
   v
RabbitMQ
   |
   v
OrderConsumer
   |
   +-- Processing failure
```

This scenario demonstrates message-processing failures.

---

## 7. Consumer Crash

Deliberately causes the RabbitMQ consumer to crash.

```http
POST /api/errors/consumer-crash
```

Expected result:

- Consumer failure.
- RabbitMQ message processing interruption.
- Error visible through application logs and monitoring.

---

## 8. High-Value Business Alert

Creates or processes an order where:

```text
Order Total > $10,000
```

This is a **business-level alert**, rather than a technical failure.

Example:

```text
Quantity x Unit Price > 10,000
```

This scenario demonstrates how observability can also be used to identify important business events.

---

## 9. Slow Message Processing

Introduces an intentional delay inside the RabbitMQ consumer.

```text
RabbitMQ
   |
   v
OrderConsumer
   |
   | Processing delay
   v
PostgreSQL
```

Expected result:

- Increased message-processing time.
- Consumer latency visible through metrics/logs.
- Useful for Dynatrace performance analysis.

---

## 10. DB Connection Pool Exhaustion

Simulates database connection exhaustion.

```http
POST /api/errors/db-connection
```

Expected result:

- Database connections become unavailable.
- Requests experience failures/timeouts.
- Dynatrace can detect database connectivity/resource issues.

---

# Logging

The Spring Boot application produces **JSON-formatted logs**.

```text
Spring Boot
     |
     v
Logback
     |
     v
JSON Logs
     |
     v
Dynatrace Agent
```

Structured JSON logging makes it easier for Dynatrace to identify:

- Exceptions
- Error messages
- Request information
- Consumer failures
- Processing delays
- Business events

---

# Monitoring

The application exposes metrics through:

```text
Spring Boot Actuator
        |
        v
Micrometer
        |
        v
Prometheus
```

Important metrics include:

- HTTP request count
- HTTP response time
- HTTP error count
- JVM metrics
- Database connection pool metrics
- RabbitMQ/message processing metrics
- Application health

---

# Infrastructure

The project uses Docker Compose for supporting infrastructure.

Expected infrastructure:

```text
+---------------------+
|     PostgreSQL      |
|      Port 5432      |
+---------------------+

+---------------------+
|      RabbitMQ       |
|      Port 5672      |
| Management: 15672   |
+---------------------+

+---------------------+
|    Spring Boot      |
|      Port 8080      |
+---------------------+
```

---

# Starting the Infrastructure

Start PostgreSQL and RabbitMQ using Docker Compose:

```bash
docker compose up -d
```

Check running containers:

```bash
docker compose ps
```

Stop the infrastructure:

```bash
docker compose down
```

---

# Running the Spring Boot Application

Run the application using Maven:

```bash
./mvnw spring-boot:run
```

Or, if Maven is installed:

```bash
mvn spring-boot:run
```

The application runs on:

```text
http://localhost:8080
```

---

# Useful Endpoints

| Endpoint | Purpose |
|---|---|
| `http://localhost:8080/api/orders` | Order REST API |
| `http://localhost:8080/actuator/health` | Application health |
| `http://localhost:8080/actuator/prometheus` | Prometheus metrics |
| `http://localhost:15672` | RabbitMQ Management UI |

RabbitMQ:

- AMQP: `5672`
- Management UI: `15672`

PostgreSQL:

- Port: `5432`

Spring Boot:

- Port: `8080`

---

# Testing the Error Flows

Once the infrastructure and Spring Boot application are running, use the error simulator endpoints to generate controlled failures.

### NullPointerException

```bash
curl -X POST http://localhost:8080/api/errors/npe
```

### Duplicate Order

```bash
curl -X POST http://localhost:8080/api/errors/duplicate-order
```

### Slow Query

```bash
curl -X POST http://localhost:8080/api/errors/slow-query
```

### Poison RabbitMQ Message

```bash
curl -X POST http://localhost:8080/api/errors/poison-message
```

### Consumer Crash

```bash
curl -X POST http://localhost:8080/api/errors/consumer-crash
```

### DB Connection Exhaustion

```bash
curl -X POST http://localhost:8080/api/errors/db-connection
```

### Optimistic Lock Conflict

```bash
curl -X POST http://localhost:8080/api/errors/optimistic-lock/1
```

---

# Observability Flow

After triggering an error:

```text
Client
  |
  v
Spring Boot Application
  |
  +-- Exception
  |
  +-- JSON Log
  |
  +-- Metrics
        |
        +---------------> Prometheus
        |
        +---------------> Dynatrace Agent
                               |
                               v
                        Error Detection
```

The purpose of Step 1 is to make sure that the application generates realistic and observable failures.

---

# Expected End-to-End Vision

```text
                         STEP 1
                 Foundation Application
                         |
                         v
             +------------------------+
             |    Spring Boot App     |
             |                        |
             | PostgreSQL + RabbitMQ  |
             | Error Simulation       |
             +-----------+------------+
                         |
                  Logs + Metrics
                         |
                         v
                         STEP 2
                  Dynatrace Agent
                         |
                         v
                  Error Detection
                         |
                         v
                ServiceNow Incident
                         |
                         v
                         STEP 3
                     AI Agent
                         |
             +-----------+-----------+
             |           |           |
             v           v           v
         Dynatrace   ServiceNow   Application
             |           |           |
             +-----------+-----------+
                         |
                         v
                Agent Coordination
```

---

# Purpose of This POC

The primary purpose of this POC is to provide a **realistic sample application that produces observable application, database, messaging and business scenarios**.

The foundation application will later be integrated with:

- **Dynatrace** for observability and problem detection.
- **ServiceNow** for incident management.
- **AI Agent** for intelligent analysis, notifications and coordination.

The application acts as the controlled environment for demonstrating the complete:

**Application Operations + Observability + ITSM + AI Agent**

workflow.

---

# Project Status

## Step 1 – Foundation Application

- [x] Spring Boot application
- [x] PostgreSQL integration
- [x] RabbitMQ integration
- [x] Order API
- [x] Order producer
- [x] Order consumer
- [x] Error simulation controller
- [x] Multiple controlled error scenarios
- [x] Audit logging
- [x] Actuator
- [x] Micrometer
- [x] Prometheus metrics
- [x] JSON logging
- [x] Docker Compose infrastructure

## Step 2 – Dynatrace

- [ ] Dynatrace agent integration
- [ ] Error detection
- [ ] Problem identification
- [ ] Automated ServiceNow incident creation

## Step 3 – AI Agent

- [ ] AI Agent integration
- [ ] Incident analysis
- [ ] Notifications
- [ ] Agent-to-agent coordination
- [ ] Automated remediation workflow
