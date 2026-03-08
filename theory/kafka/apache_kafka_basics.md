# Apache Kafka: The Basics

## What is Apache Kafka?
Apache Kafka is an open-source distributed event streaming platform used by thousands of companies for high-performance data pipelines, streaming analytics, data integration, and mission-critical applications. In simpler terms, it's a highly scalable and fault-tolerant system that allows you to publish (write) and subscribe to (read) streams of events/messages in real-time.

## Why do we need Kafka?
Before Kafka, as architectures grew, different systems needed to communicate with each other. A source system would need to send data to a target system. As the number of sources and targets increased, the number of integrations became a tangled web (an $O(N^2)$ problem). 

Kafka solves this by decoupling the data streams and acting as a central hub:
1. **Decoupling**: The producer doesn't need to know who the consumer is, and vice versa. They only need to know about Kafka.
2. **High Throughput & Low Latency**: It can handle millions of messages per second with minimal latency (often less than 10ms).
3. **Scalability**: It runs as a cluster of one or more servers that can span multiple datacenters. You can scale it horizontally by just adding more brokers.
4. **Durability & Fault Tolerance**: Messages are persisted on disk and replicated within the cluster to prevent data loss in case a server crashes.

**Common Use Cases:**
- Message broker (similar to RabbitMQ but built for larger scale).
- Activity tracking (e.g., user clicks on a website sent to analytics).
- Metrics and logging gathering (centralizing logs from many microservices).
- Stream processing (acting on data as it arrives, e.g., fraud detection).

---

## Core Concepts

* **Event/Message**: A record of something that happened (e.g., "User Alice signed up"). Think of it like a row in a database table or a JSON object.
* **Topic**: A category or feed name to which events are published. Think of it like a folder in a filesystem, or a table in a database. Topics in Kafka are multi-producer and multi-subscriber.
* **Producer**: Client applications that publish (write) events to Kafka topics.
* **Consumer**: Client applications that subscribe to (read) events from Kafka topics.
* **Broker**: A single Kafka server. A Kafka cluster is made up of multiple brokers running together.
* **Partition**: Topics are broken down into partitions for scalability. A partition is an ordered, immutable sequence of records. Partitions allow a single topic to be spread across multiple brokers.
* **Offset**: A unique identifier (an incremental ID) assigned to each message within a partition. It tells consumers exactly where they are currently reading from.
* **Consumer Group**: A group of consumers that cooperate to consume messages from a topic. Each partition connects to exactly one consumer within a group, allowing parallel processing.

---

## Simple Code Example (Java)

Since you are working with Java in your project, here is a basic example of how to write a Producer and a Consumer using the raw Java Kafka client.

### 1. Maven Dependency (`pom.xml`)
If you were to create a project for this, you would add these dependencies:
```xml
<dependencies>
    <!-- Kafka Client Dependency -->
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-clients</artifactId>
        <version>3.6.1</version>
    </dependency>
    <!-- SLF4J for logging (required by Kafka) -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>2.0.9</version>
    </dependency>
</dependencies>
```

### 2. The Producer (`SimpleProducer.java`)
The producer sends messages to a specific topic (`learning-kafka`).

```java
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class SimpleProducer {
    public static void main(String[] args) {
        String bootstrapServers = "127.0.0.1:9092"; // Address of your Kafka Broker
        String topic = "learning-kafka";

        // 1. Create Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // 2. Create the Producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // 3. Send data
        for (int i = 0; i < 10; i++) {
            String message = "Hello Kafka message " + i;
            // Create a record (Message)
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, message);
            
            // Send Data (Asynchronous)
            producer.send(record);
            System.out.println("Sent: " + message);
        }

        // 4. Flush and close producer
        producer.flush(); // Wait for data to be sent
        producer.close();
    }
}
```

### 3. The Consumer (`SimpleConsumer.java`)
The consumer polls (constantly asks) for new messages from the topic.

```java
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class SimpleConsumer {
    public static void main(String[] args) {
        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "my-first-application";
        String topic = "learning-kafka";

        // 1. Create Consumer Properties
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        // "earliest" means read from the beginning if there's no previous offset found
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); 

        // 2. Create the Consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // 3. Subscribe consumer to our topic
        consumer.subscribe(Collections.singletonList(topic));

        // 4. Poll for new data in an infinite loop
        System.out.println("Polling for messages...");
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, String> record : records) {
                System.out.println("Received: Key: " + record.key() + ", Value: " + record.value());
                System.out.println("Partition: " + record.partition() + ", Offset: " + record.offset());
            }
        }
    }
}
```
