# Day 1: Servers and Load Balancers

Welcome to Day 1 of our System Design journey! Today we are discussing fundamental concepts: **Servers** and **Load Balancers**.

## What is a Server?
In simple terms, a **Server** is a powerful computer or system that provides resources, data, services, or programs to other computers (known as **clients**) over a network. 

**Analogy:** Imagine a restaurant. 
- You (the customer) are the **Client**.
- The waiter who takes your order and brings you food is the **Server**.
- The food you request is the **Data/Response**.

When you open an app like Instagram, your phone (Client) sends a request over the internet to Instagram's computers (Servers) to fetch your feed. The servers process this request and send the images and videos back to your phone.

## The Real Problem Server Architectures Face
When an application is first built, placing everything on a single server (database, application logic, web server) is usually enough. 

However, as the application becomes popular:
1. **Traffic Spikes:** Too many users try to access the application at the same time. The single server gets overwhelmed.
2. **Performance Issues:** Since the server can only handle a limited number of requests per second, the app becomes slow and unresponsive.
3. **Single Point of Failure (SPOF):** If that one server crashes or goes offline for maintenance, the entire application goes down. 

To solve this, we add more servers. This is called **Horizontal Scaling** (adding more machines to the pool of resources). But now, there is a new problem: *How does a user know which server to talk to?*

## Enter: The Load Balancer
A **Load Balancer** is a device or software that acts as a "traffic cop" sitting in front of your group of servers. 

When a client sends a request, it hits the Load Balancer first. The Load Balancer then efficiently distributes (routes) that incoming network traffic across a group of backend servers.

### How it Solves the Problem:
1. **Distributes Traffic Evenly:** It ensures no single server bears too much demand. By spreading the work evenly, it prevents any server from getting overloaded or crashing.
2. **High Availability and Reliability:** If a server goes down, the Load Balancer immediately redirects traffic to the remaining online servers. When a new server is added, the Load Balancer easily starts sending requests to it.
3. **Flexibility:** You can perform maintenance on a server by taking it offline without users ever knowing, because the Load Balancer will just route traffic to other servers.

### Common Routing Algorithms used by Load Balancers:
- **Round Robin:** Requests are distributed sequentially to the group of servers (Server 1, then Server 2, then Server 3, then back to Server 1).
- **Least Connections:** Sends the new request to the server that currently has the fewest active connections.
- **IP Hash:** The IP address of the client is used to determine which server receives the request. This ensures a specific user always connects to the same server.

## Summary
A Load Balancer is essential for any modern application that requires high availability, reliability, and the ability to scale to handle massive amounts of user traffic.
