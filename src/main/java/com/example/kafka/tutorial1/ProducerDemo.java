package com.example.kafka.tutorial1;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.LocalDateTime;
import java.util.Properties;

public class ProducerDemo {

    public static void main(String[] args) throws InterruptedException {
        String bootStrapServers = "127.0.0.1:9092";

        // Create Producer properties
        Properties properties = new Properties();
        // old way
        // properties.setProperty("bootstrap.servers", "127.0.0.1:9092");
        // properties.setProperty("key.serializer", StringSerializer.class.getName());
        // properties.setProperty("value.serializer", StringSerializer.class.getName());
        // new way
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create the producer
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);

        // create a Producer record
        ProducerRecord<String, String> record = new ProducerRecord<>("first_topic", "Hello World from Java Kafka! " + LocalDateTime.now());

        // send data - asynchronous
        kafkaProducer.send(record);

        // flush data
        kafkaProducer.flush();

        // flush and close producer
        kafkaProducer.close();

    }
}
