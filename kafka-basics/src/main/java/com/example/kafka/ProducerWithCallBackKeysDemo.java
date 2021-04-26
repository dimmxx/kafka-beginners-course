package com.example.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ProducerWithCallBackKeysDemo {

    private static final Logger logger = LoggerFactory.getLogger(ProducerWithCallBackKeysDemo.class);

    public static void main(String[] args) {

        String bootStrapServers = "127.0.0.1:9092";

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);

        Map<Integer, Integer> partitions = new HashMap<>();// inspect what partitions are written to

        for (int i = 0; i < 10; i++) {

            String key = "id_" + i;
            String topic = "first_topic";
            String message = "Message #" + i;

            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);

//            kafkaProducer.send(record, new Callback() {
//                @Override
//                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
//                    // executes every time a record is successfully sent or an exception is thrown
//                    logRecord(recordMetadata, e, message, key, partitions);
//                }
//            });

            // the same with lambda
            kafkaProducer.send(record, (recordMetadata, e) -> logRecord(recordMetadata, e, message, key, partitions));
        }

        // flush and close producer
        kafkaProducer.close();

        partitions.forEach((k, v) -> System.out.println(k + ": " + v));
        System.out.println("total: " + partitions.values().stream().reduce((l, r) -> l + r).orElse(-1));
    }

    private static void logRecord (RecordMetadata recordMetadata, Exception e, String message, String key, Map<Integer, Integer> map){
        if (e == null) { // record is successfully sent
            logger.info(
                    "Received new metadata\n" +
                            "Key: " + key + "\n" +
                            "Message: " + message + "\n" +
                            "Topic: " + recordMetadata.topic() + "\n" +
                            "Partition: " + recordMetadata.partition() + "\n" +
                            "Offset: " + recordMetadata.offset() + "\n" +
                            "Timestamp: " + recordMetadata.timestamp() +
                            " " + LocalDateTime.now() + "\n"
            );

            if(map.containsKey(recordMetadata.partition())){
                map.put(recordMetadata.partition(), map.get(recordMetadata.partition()) + 1);
            }else {
                map.put(recordMetadata.partition(), 1);
            }
        } else {
            logger.error("Error while producing", e);
        }
    }
}
