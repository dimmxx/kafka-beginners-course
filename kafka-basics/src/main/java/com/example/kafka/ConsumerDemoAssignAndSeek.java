package com.example.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class ConsumerDemoAssignAndSeek {

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(ConsumerDemoAssignAndSeek.class);

        String bootStrapServers = "127.0.0.1:9092";
        String topic = "first_topic";

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        // assign and seek are mostly used to replay data or fetch a specific message

        TopicPartition topicPartitionToReadFrom = new TopicPartition(topic, 0);
        consumer.assign(Collections.singletonList(topicPartitionToReadFrom));

        consumer.seek(topicPartitionToReadFrom, 0L);

        int numberOfMessagesToRead = 5;
        boolean keepOnReading = true;
        int messagesRead = 0;

        while (keepOnReading) {
            ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, String> record : consumerRecords) {
                messagesRead += 1;
                logger.info(
                        "Received new metadata\n" +
                                "Key: " + record.key() + "\n" +
                                "Value: " + record.value() + "\n" +
                                "Topic: " + record.topic() + "\n" +
                                "Partition: " + record.partition() + "\n" +
                                "Offset: " + record.offset() + "\n"
                );
                if(messagesRead >= numberOfMessagesToRead){
                    keepOnReading = false;
                    break;
                }
            }
        }
        logger.info("Exiting the application");
    }
}
