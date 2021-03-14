package com.example.kafka.tutorial1;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ProducerKeysDemo {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        Logger logger = LoggerFactory.getLogger(ProducerKeysDemo.class);

        String bootStrapServers = "127.0.0.1:9092";

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);

        Set<Integer> partitions = new HashSet<>(); // inspect what partitions are written to

        for(int i = 0; i < 10; i++){

            String topic = "first_topic";
            String value = "kafka message # " + Integer.toString(i);
            String key = "id_" + Integer.toString(i);

            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

            logger.info("Key: " + key);

            kafkaProducer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    // executes every time a record is successfully sent or an exception is thrown
                    if(e == null){ // record is successfully sent
                        logger.info(
                                "Received new metadata\n" +
                                        "Topic: " + recordMetadata.topic() + "\n" +
                                        "Partition: " + recordMetadata.partition() + "\n" +
                                        "Offset: " + recordMetadata.offset() + "\n" +
                                        "Timestamp: " + recordMetadata.timestamp() +
                                        " " + LocalDateTime.now() + "\n"
                        );
                        partitions.add(recordMetadata.partition());
                    } else {
                        logger.error("Error while producing", e);
                    }
                }
            }).get(); // block the .send() to make it synchronous - do not do this in production
        }

        // flush data
        //kafkaProducer.flush();

        // flush and close producer
        kafkaProducer.close();

        partitions.forEach(System.out::println);

    }
}
