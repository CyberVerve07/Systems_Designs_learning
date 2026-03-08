package com.systemdesign.kafka.basic;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class SimpleProducer {
    private static final Logger log = LoggerFactory.getLogger(SimpleProducer.class);

    public static void main(String[] args) {
        log.info("I am a Kafka Producer");

        String bootstrapServers = "127.0.0.1:9092";
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
            String message = "Hello Kafka message number " + i;
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, message);
            
            // Send Data (Asynchronous)
            producer.send(record, (recordMetadata, e) -> {
                // executes every time a record is successfully sent or an exception is thrown
                if (e == null) {
                    log.info("Received new metadata. \n" +
                            "Topic:" + recordMetadata.topic() + "\n" +
                            "Partition: " + recordMetadata.partition() + "\n" +
                            "Offset: " + recordMetadata.offset() + "\n" +
                            "Timestamp: " + recordMetadata.timestamp());
                } else {
                    log.error("Error while producing", e);
                }
            });
            
            try {
                // sleep for a brief moment to see messages stream gradually
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // 4. Flush and close producer
        producer.flush(); 
        producer.close();
        log.info("Producer has finished sending messages and is closed.");
    }
}
