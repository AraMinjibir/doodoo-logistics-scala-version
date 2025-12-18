DooDoo Logistics

Play Framework • Scala • PostgreSQL • REST  • WebSockets • Event Sourcing
DooDoo Logistics is a production-ready logistics and delivery management platform inspired by real-world
systems such as UPS, DHL, Bolt Logistics, DoorDash, and Glovo.
This project is designed as a portfolio-grade showcase of large-scale engineering skills for backend,
distributed systems, data engineering, and full-stack roles.
The project focuses on building, testing, and hardening a real backend system using industry-standard
Scala and Play Framework practices before introducing any unnecessary complexity.
Its primary goal is to demonstrate how a typical logistics system is designed, implemented, tested, and
prepared for production in a realistic, non-distributed environment,
reflecting how most systems are actually built and deployed.
1. Problem Statement
A typical logistics platform must reliably handle:
Shipment creation and lifecycle management
Tracking shipments via unique tracking numbers
Controlled status transitions (Created → In Transit → Delivered)
Clear separation of write and read responsibilities
Data consistency and validation
Audit history of shipment state changes
Admin and support workflows
Clear failure handling and error reporting
Testability and maintainability
**2. Shipment Management (Sender / Customer)
Create shipments with full validation
Generate unique tracking numbers
Retrieve shipments by:
Tracking number
Shipment ID
View current shipment status and history
Receive clear error feedback for invalid or missing shipments
Service Provider Features
Service Providers represent delivery agents responsible for moving shipments through their lifecycle.
Accept assigned shipments
Update shipment status:
Created → In Transit
In Transit → Delivered
Submit delivery completion details (timestamp, optional notes)
View assigned shipments
Access shipment history relevant to their deliveries
Status transitions enforced strictly by business rules
Focus: correctness of workflow, authorization boundaries, and state validation — not scale.
Admin Features
Admins oversee system operations and ensure data integrity.
View all shipments in the system
Query shipments by:
Status
Date range
Tracking number
Inspect full shipment lifecycle history
Correct operational issues (e.g. resolve stuck shipments)
Monitor failed or invalid state transitions
Manage shipment records (controlled deletion / correction)
Shipment Lifecycle & Business Rules
Explicit domain states:
Created
In Transit
Delivered
Role-based actions:
Senders create shipments
Service Providers progress shipments
Admins oversee and audit
Invalid transitions are rejected at the service layer
Every state change is recorded in shipment history
Audit & History
Full timeline of shipment state changes
Timestamped events for each transition
Location and metadata support
Enables:
Debugging
Customer support
Operational review
Architecture Overview (Current)
This project is intentionally designed as a modular monolith, reflecting real production systems.

```|   API Clients          |
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
Key Design Principles

Clear separation of concerns

Domain logic isolated from persistence

Repositories abstracted and mockable

Services fully unit tested

Ready for production hardening

4. Technologies Used
Backend

Scala

Play Framework

PostgreSQL

Futures & asynchronous processing

JSON APIs

Testing

ScalaTest

Mockito

Unit tests for:

Services

Validation

Error paths

Repository and integration testing (in progress)

Planned end-to-end tests

Ops (Incremental)

sbt

Docker (local development)

Logging & error handling (in progress)

5. Testing Strategy (Current Focus)

This project prioritizes testing correctness before scale.

Implemented

Service-level unit tests

Success paths

Validation failures

Not-found cases

Invalid state transitions

Mocked repositories and dependencies

Deterministic fixtures for repeatable tests

Next Steps

Repository tests against PostgreSQL

Integration tests (API → DB)

End-to-end tests for shipment workflows

Production-grade error handling

Logging and observability

6. Running the Project
git clone git@gitlab.com:AraMjb/doodoo-logistics.git
cd doodoo-logistics
sbt run