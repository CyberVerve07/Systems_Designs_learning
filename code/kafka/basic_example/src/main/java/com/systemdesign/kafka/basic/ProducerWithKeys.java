package com.systemdesign.kafka.basic;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * Demonstrates how to send messages with a Key.
 * Messages with the same key are guaranteed to go to the SAME partition,
 * thus maintaining their order within that partition.
 */
public class ProducerWithKeys {
    private static final Logger log = LoggerFactory.getLogger(ProducerWithKeys.class);

    public static void main(String[] args) {
        String bootstrapServers = "127.0.0.1:9092";
        String topic = "learning-kafka";

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        for (int i = 0; i < 20; i++) {
            // Using a key: User_ID_1 for even messages, User_ID_2 for odd messages
            String key = "id_" + (i % 2 == 0 ? "1" : "2");
            String value = "Update for " + key + " - counter: " + i;

            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

            producer.send(record, (metadata, e) -> {
                if (e == null) {
                    log.info("Key: " + key + " | Partition: " + metadata.partition() + " | Offset: " + metadata.offset());
                } else {
                    log.error("Error while producing", e);
                }
            });
        }

        producer.flush();
        producer.close();
        log.info("Producer with Keys has finished.");
    }
}
