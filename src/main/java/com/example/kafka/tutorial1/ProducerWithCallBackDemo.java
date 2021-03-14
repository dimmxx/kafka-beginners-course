package com.example.kafka.tutorial1;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class ProducerWithCallBackDemo {

    public static void main(String[] args) throws InterruptedException {

        Logger logger = LoggerFactory.getLogger(ProducerWithCallBackDemo.class);

        String bootStrapServers = "127.0.0.1:9092";

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);

        Set<Integer> partitions = new HashSet<>(); // inspect what partitions are written to

        for(int i = 0; i < 10000; i++){
            ProducerRecord<String, String> record = new ProducerRecord<>("first_topic", "Message # " + i);
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
            });
        }

        // flush data
        //kafkaProducer.flush();

        // flush and close producer
        kafkaProducer.close();

        partitions.forEach(System.out::println);

    }
}
