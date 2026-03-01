# Notification System Design (Theory)

Today we are discussing the core theoretical concepts needed to build a highly available and scalable **Notification System**. 

The code portion and diagrams will be done later in our newly created `day2_diagrams` folder, but first, let's understand the underlying components.

---

### 1. Event-Driven Architecture (EDA)
**Event-Driven Architecture** is a software design pattern where decoupled systems/services communicate by generating and responding to "events." 
An event is just a record of a state change (e.g., "User Registered", "Payment Successful", "Order Placed"). Instead of the Order Service directly telling the Notification Service "send an email", it simply announces the event. The Notification Service listens for this event and acts on its own. This makes the system loosely coupled and highly resilient.

### 2. Pub/Sub Architecture
**Pub/Sub (Publisher-Subscriber)** is a specific messaging pattern often used in Event-Driven Architecture.
- **Publishers:** The services that create the event (e.g., Payment Service) and send it to a central message broker, without knowing who will receive it.
- **Subscribers:** The services (e.g., Notification Service, Analytics Service) that express interest in specific events. They "subscribe" to a topic and react when a publisher sends a message to that topic.

### 3. Message Brokers & Queues (RabbitMQ vs. Kafka)
When a massive burst of notifications needs to go out (like breaking news), we can't process them all instantly. We put them in a line (a queue or topics) so we can process them at our own pace.
- **RabbitMQ:** A traditional message broker that uses a smart routing system (Exchanges and Queues) to push messages to consumers. Once a message is consumed successfully, it gets deleted. Great for standard task-routing and notifications.
- **Kafka:** A distributed event streaming platform. Instead of deleting messages once read, Kafka appends them to a log. Multiple consumers can pull and read the same messages at their own pace. Great for massive-scale pipelines, tracking user activity, and event sourcing.

### 4. Workers
**Workers** (or Background Processors) are simply applications designed to continuously pull or listen to tasks from the message queues (RabbitMQ/Kafka) and execute the actual heavy lifting.
For a notification system, a worker would:
1. Parse the event message ("Send SMS to User X").
2. Query the DB if necessary to get the phone number/preferences.
3. Call the actual third-party API (Twilio, AWS SES, Firebase).
Workers run independently from your main web servers, preventing external API slowness from blocking your API responses.

### 5. EC2 Machines
**EC2 (Elastic Compute Cloud)** is a web service from Amazon Web Services (AWS) that provides virtual servers in the cloud. You deploy your APIs and Workers onto these EC2 instances. They provide the actual CPU and RAM needed to run your code. 

### 6. How Servers Automatically Scale (Auto-Scaling)
If an application goes viral, a fixed number of EC2 machines wouldn't handle the load.
Cloud providers use **Auto-Scaling Groups (ASG)**. 
- You set up a "metric" monitoring rule (e.g., "If average CPU usage across servers goes above 70%", or "If the RabbitMQ queue has more than 10,000 pending messages").
- When the condition is met, the Auto-Scaler automatically spins up new fresh EC2 instances (Scale Out) and puts them behind the Load Balancer, or adds more Workers to drain the queue faster.
- When traffic dies down, it terminates the extra instances (Scale In) to save money.

### 7. S2S (Server-to-Server) Communication
**S2S** simply means back-end servers talking to other back-end servers, rather than a client (browser/mobile app) talking to a server.
In a notification system, your internal microservices (like the User Service checking preferences) communicate with the Notification Service. This is usually done either synchronously (REST API / gRPC calls) or asynchronously (via Kafka/RabbitMQ).

### 8. Rate Limiting
**Rate Limiting** is the practice of restricting the number of requests a user or a service can make in a given timeframe.
Why do we need it in notifications?
- **Avoid Spam:** You don't want to accidentally send a user 10 promos in 1 minute.
- **Provider Limits:** 3rd party providers like SendGrid or Twilio enforce limits on your API keys (e.g., 500 emails/second). If you exceed it, they block you. Your system must throttle the outgoing messages to respect these limits.

### 9. Firebase Push Notification (FCM)
**FCM (Firebase Cloud Messaging)** is a Google service that helps you send push notifications to mobile devices (Android/iOS) and web browsers reliably and for free.
- **Why we need it:** Mobile phones cannot keep dedicated, open network connections to *every* single app backend (WhatsApp, Instagram, your app). It would drain the battery instantly. 
- Instead, the phone keeps *one* single optimized connection alive with Apple (APNs) or Google (FCM). Your backend sends the payload to FCM, and FCM handles the complexity of accurately waking up the device and delivering the push notification over that single connection.
