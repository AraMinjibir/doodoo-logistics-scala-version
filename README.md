# DooDoo Logistics — Distributed Shipment Tracking Platform

**Pekko Cluster • Kafka • Play Framework • WebSockets • Event Sourcing**

DooDoo Logistics is a distributed logistics and delivery management platform inspired by real-world systems such as UPS, 
DHL, Bolt Logistics, Doordash, and Glovo. It demonstrates how modern companies build scalable and fault-tolerant delivery
infrastructure using distributed systems, event-driven architectures, real-time messaging, and resilient stateful services.

This project is designed as a portfolio-grade showcase of large-scale engineering skills for backend, 
distributed systems, data engineering, and full-stack roles.

**1. Problem Statement**

Modern logistics platforms must handle:

Real-time tracking of thousands of active shipments

Multi-stage delivery pipelines across cities/regions

Consistency guarantees across distributed environments

Event-driven state transitions for pickup, transit, and delivery

Live GPS updates from mobile devices

System resilience to failures, retries, and network partitions

High-throughput ingestion of events

Monitoring and support operations for customers

Full audit trails for compliance and analytics

DooDoo Logistics solves these challenges using:

**Pekko Cluster Sharding for distributed stateful services**

**Kafka for decoupled event processing**

**Event Sourcing for fault tolerance and auditability**

**Pekko Streams for GPS ingestion and real-time streaming**

**Play Framework for scalable API endpoints**

WebSockets for live dashboards

**2. Core Features**
   Distributed Shipment Engine

ShipmentActor (sharded): Each shipment is an independent event-sourced entity

Automatic cluster rebalancing on node join/leave

Horizontal scalability and fault tolerance

Durable state recovery through event logs

Event-Driven Delivery Pipeline (Kafka)

Kafka topics include:

shipment-created

pickup-started

in-transit

delivered

shipment-exception

support-ticket-created

support-ticket-resolved

All domain changes flow through Kafka, enabling:

Loose coupling

Real-time analytics

Replayable pipelines

Multi-subscriber architecture

GPS Tracking via Pekko Streams

Service provider mobile apps push GPS updates

Stream processing reduces incoming data

Real-time location updates fed to WebSockets

Position history stored via event sourcing

Play Framework REST API

**Endpoints include:**

Sender

Create shipment

Retrieve current shipment state

Query historical events

Service Provider

Accept pickup

Update delivery stage

Push GPS coordinates

Administrator

Query all shipments (materialized projections)

Filter shipments by status/state/area

View event timeline for any shipment

Support Agent (Newly Added)

Create customer support tickets

Attach inquiries/questions to a shipment

Resolve tickets

View ticket history (event-sourced)

Subscribe to support-ticket events via WebSockets

Real-Time WebSocket Notifications

Live shipment map feed

Stage transitions: Pickup → In Transit → Delivered

Exception alerts

Support ticket updates

Admin dashboard data refresh

Event-Sourced History

Every single event is persisted:

Commands

State transitions

Location updates

Support agent ticket events

Admin actions

Full audit trail enables:

Time-travel debugging

Compliance pipelines

ML training datasets

Admin Dashboard (Optional Frontend)

Real-time shipment map

Ticketing console for Support Agents

Streaming Kafka-driven analytics

**3. Architecture Overview**
   +---------------------------+
   |   Sender / Recipient App  |
   |   Service Provider App    |
   |   Admin & Support UI      |
   +-------------+-------------+
   |
   REST / WS
   |
   +-------+--------+
   |  Play Framework |
   |    API Layer    |
   +-------+---------+
   |
   Commands / Queries
   |
   +-------------+-----------------------------+
   |         Pekko Cluster (Multi-Node)        |
   |-------------------------------------------|
   |  ShipmentActor (Sharded, Event-Sourced)   |
   |  ServiceProviderActor                     |
   |  SupportTicketActor (Event-Sourced)       |
   |  NotificationActor                         |
   |  RouteSupervisorActor                      |
   +-------------+-----------------------------+
   |
   | Events (Kafka)
   v
   +-----------+
   |   Kafka   |
   +-----------+
   |
   Stream Processing / ETL
   |
   +-----------+
   | Postgres  |
   | Read Side |
   +-----------+

**4. Technologies Used**
   Backend

Pekko Cluster & Sharding

Pekko Persistence (Event Sourcing)

Pekko Streams

Apache Kafka

Play Framework (Scala)

PostgreSQL (Materialized views)

WebSockets (Play + Pekko Streams)

JSON or Protobuf

Ops

Docker Compose

GitHub Actions CI/CD

Kubernetes-ready deployment manifests

Frontend (Optional for demo)

Angular or React-based admin/support dashboard

Real-time WebSocket updates

**5. Use Cases**
1. Create Shipment

User submits shipment

ShipmentActor persists ShipmentCreated

Kafka publishes shipment-created

WebSocket pushes notifications

2. Assign Service Provider

Admin assigns or system auto-assigns

ServiceProviderAssigned event stored

Dashboard updated in real-time

3. Pickup Workflow

Provider accepts pickup

Provider collects package

Shipment state transitions: PickupStarted, PickedUp

4. Transit Workflow

Provider sends GPS updates

Map updates via WebSocket

State transitions: InTransit

5. Delivery Workflow

Deliver to recipient

Signature/photo (metadata stored as event)

Final event: Delivered

6. Support Agent Features
   Create support ticket

Support agent creates inquiry tied to shipment or user

Event: SupportTicketCreated

Kafka publishes ticket event

Admin & Support dashboard update live

Add internal notes / responses

Events: SupportTicketUpdated

Resolve ticket

Event: SupportTicketResolved

WebSocket pushes resolution update

Query ticket history

Read-side provides full timeline (event-sourced)

Monitor critical shipment exceptions

Support Agent automatically receives:

Delivery failures

GPS offline alerts

Exception events

Escalation notifications

6. Running the Project
   git clone https://github.com/AraMjb/doodoo-logistics
   cd doodoo-logistics
   docker-compose up -d
   sbt run


This starts:

Kafka

Zookeeper

PostgreSQL

Play API Server

Pekko Cluster Nodes

7. Project Goals

Demonstrate practical distributed-systems engineering

Showcase mastery of:

Cluster Sharding

Event Sourcing

CQRS

Real-time streaming

Actor supervision

Event-driven pipelines

Observability at scale

Provide a reference architecture for real companies

**8. Status**

The project is being redesigned from an earlier Firebase-based architecture into a fully distributed enterprise-grade platform based on Pekko, 
Kafka, Play, and PostgreSQL.

Upcoming tasks include:

Shipment command model refinement

Materialized read-side projections

WebSocket stream consolidation

SupportTicketActor finalization

Route optimization (future extension)