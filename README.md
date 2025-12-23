# DooDoo Logistics 

**Play Framework • Scala • PostgreSQL • REST  • WebSockets • Event Sourcing**

DooDoo Logistics is a production-oriented logistics and delivery management backend inspired by industrial-grade systems like DHL, Bolt Logistics, and FedEx. Built as a Modular Monolith, this project demonstrates how to handle complex business rules, strict state transitions, and asynchronous event processing within a maintainable and testable architecture.

The project focuses on building, testing, and hardening a real backend system using industry-standard
 Scala and Play Framework practices before introducing any unnecessary complexity.

Its primary goal is to demonstrate how a typical logistics system is designed, implemented, tested, and
 prepared for production in a realistic, non-distributed environment, 
reflecting how most systems are actually built and deployed.

**1. Problem Statement**

Logistics platforms must maintain 100% data consistency and operational transparency. DooDoo Logistics addresses these challenges through:

Deterministic Lifecycle Management: Preventing illegal state jumps (e.g., jumping from Created to Delivered without being In Transit).

Role-Based Access Control (RBAC): Defining clear boundaries between Customers, Service Providers, Support Agents, and Admins.

Auditability: A permanent, timestamped history of every shipment status change.

Event-Driven Notifications: Ensuring stakeholders are updated in real-time without blocking the core API performance.

2. Core Capabilities by Role
The system enforces the Principle of Least Privilege (PoLP) across four distinct roles:

Customer / Sender
• Shipment Management: Create shipments with full input validation and receive unique tracking 
  numbers.

• Visibility: Real-time tracking and access to the complete lifecycle history of their packages.

Recipient (The Consignee)
• Inbound Visibility: Track all incoming packages linked to the user's verified identity (phone/ 
   email).

• Dynamic Delivery Instructions: Provide real-time notes to Service Providers to ensure 
  successful first-time delivery.

• Delivery Confirmation: Access official Proof of Delivery (PoD) data once a shipment reaches the 
  final state.

Service Provider (Courier)
• Logistics Progression: Responsible for moving shipments through active states (Accept, In 
  Transit, Delivered).

• Metadata Entry: Submitting delivery notes and proof-of-delivery timestamps.

Support Agent (Operational Integrity)
• Advanced Observability: Search and filter shipments by tracking number, status, or date range 
 to resolve inquiries.

• Ticket Management: Creating and managing support tickets linked to specific shipments and users.

• Anomaly Detection: Identifying "stuck" shipments that have failed to progress within expected 
  SLAs.

Administrator
• System Oversight: Full read/write access to all entities.

• Operational Recovery: Authorized to perform manual state corrections and audit the entire 
 system's health.

3. Architecture & Design Patterns
The system follows Domain-Driven Design (DDD) principles to isolate business logic from infrastructure.

• Persistence Ignorance: Service layers interact with Repository traits, allowing the underlying 
  database (PostgreSQL) to be swapped or mocked for testing.

• Event-Driven Architecture (EDA): Utilizing Kafka to decouple the core logistics engine from the 
  notification system.

• Type-Safe Domain: Leveraging Scala's sealed traits and Enumeratum to ensure that only valid 
  shipment statuses and roles can exist at compile-time.

```+------------------------+
|   API Clients          |
|  (Web / Mobile / Admin)|
+-----------+------------+
            |
         REST / WS
            |
+-----------+------------+
| Play Framework API     |
| Controllers            |
+-----------+------------+
            |
      Service Layer
            |
+-----------+------------+
| Domain Logic           |
| Validation             |
| Business Rules         |
+-----------+------------+
            |
      Repository Layer
            |
+-----------+------------+
| PostgreSQL Database    |
+------------------------+```

4. Technical Stack

• Backend: Scala, Play Framework (Asynchronous/Non-blocking I/O).

• Persistence: PostgreSQL with Slick (Type-safe SQL DSL).

• Messaging: Apache Kafka for reliable, asynchronous notification delivery.

• Security: BCrypt password hashing and JWT-based authentication.

5. Testing Strategy
DooDoo Logistics prioritizes correctness through a comprehensive testing pyramid:

• Service-Level Unit Tests: 100% coverage of business rules and state transition logic using 
  Mockito.

• Integration Tests: Verifying Repository-to-Database mapping and SQL query correctness.

• End-to-End (E2E) Tests: Full-flow verification simulating a shipment's journey from initial 
 creation to final delivery, ensuring all layers (API, Service, Repo) work in harmony.

6. Event-Driven Notifications
To maintain high throughput, notification logic is offloaded to Kafka:

• Produce: When a shipment status changes, a StatusChanged event is published to Kafka.

• Consume: A dedicated Notification Consumer listens for these events.

• Execute: The consumer triggers Email or Push notifications via external providers (e.g., 
  SendGrid/Firebase).

7. Roadmap & Operational Readiness

[x] Core Shipment Engine: Basic creation, tracking, and persistence.

[x] RBAC Implementation: Secure boundaries for all user roles.

[x] Support Module: Ticket management and internal auditing.

[ ] Kafka Integration: Implementation of the Event Producer and Notification Consumer.

[ ] Observability: Structured logging (SLF4J) and health check endpoints for production monitoring.

[ ] CI/CD Pipeline: Automated GitHub Actions/GitLab CI for testing and Docker deployment.

8. Running the Project Locally
Bash

# Clone the repository
git clone git@gitlab.com:AraMjb/doodoo-logistics.git

# Ensure PostgreSQL and Kafka are running via Docker
docker-compose up -d

# Run the Play application
sbt run