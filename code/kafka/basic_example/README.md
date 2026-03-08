# Kafka Basic Example

This project demonstrates a very simple Apache Kafka Producer and Consumer using plain Java. 
We'll run a local single-node Kafka cluster using Docker.

## Prerequisites
1. **Docker Desktop** installed and running.
2. **Java JDK 17+** installed.
3. **Maven** installed (or use your IDE's built-in Maven).
4. An IDE like IntelliJ IDEA, Eclipse, or VS Code to easily run the Java main methods.

## Step 1: Start Kafka using Docker

Navigate to this directory in your terminal and run:

```bash
docker-compose up -d
```
This will download the `confluentinc/cp-zookeeper` and `confluentinc/cp-kafka` images and start two containers in the background.

To verify they are running, you can use:
```bash
docker ps
```
You should see both `kafka` and `zookeeper` containers running.

## Step 2: Run the Consumer

It is usually a good idea to start the consumer first, so it is waiting and listening when the producer starts sending messages.

1. Open this project folder (`c:\Users\lenovo\Desktop\Systems_Designs_learning\code\kafka\basic_example`) in your IDE.
2. Your IDE should recognize it as a Maven project (thanks to the `pom.xml`). Make sure to reload/sync the Maven dependencies.
3. Navigate to `src/main/java/com/systemdesign/kafka/basic/SimpleConsumer.java`.
4. Run the `main` method in `SimpleConsumer`.

You will see log output indicating that the consumer has subscribed to the topic `learning-kafka` and is polling for messages. 

## Step 3: Run the Producer

While the consumer is still running (don't stop it!):
1. In your IDE, navigate to `src/main/java/com/systemdesign/kafka/basic/SimpleProducer.java`.
2. Run the `main` method in `SimpleProducer`.

### What happens?
- The Producer will send 10 messages (e.g., "Hello Kafka message number 0", "Hello Kafka message number 1", etc.) with a small 500ms delay between each.
- In your IDE, switch back and forth between the console output for the Producer and the console output for the Consumer.
- You will see the Producer logging that it successfully sent the messages to partition 0 with specific offsets.
- You will see the Consumer immediately picking up these messages and printing their values to the console!

## Stop Cleanup

Once you are done playing around, you can stop the Kafka and Zookeeper containers and remove them by running:

```bash
docker-compose down
```
This will cleanly shut down the containers.
