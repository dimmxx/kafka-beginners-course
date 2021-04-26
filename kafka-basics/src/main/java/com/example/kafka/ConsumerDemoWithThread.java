package com.example.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerDemoWithThread {

    private ConsumerDemoWithThread() {
    }

    public static void main(String[] args) {
        new ConsumerDemoWithThread().launch();
    }

    private void launch() {
        Logger logger = LoggerFactory.getLogger(ConsumerDemoWithThread.class);
        String bootStrapServers = "127.0.0.1:9092";
        String groupId = "my-sixth-application";
        String topic = "first_topic";
        CountDownLatch latch = new CountDownLatch(1);

        logger.info("Creating the consumer thread");
        ConsumerRunnable myConsumerRunnable = new ConsumerRunnable(bootStrapServers, groupId, topic, latch);
        Thread thread = new Thread(myConsumerRunnable);
        thread.start();

        // add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Caught shutdown hook[1]");
            myConsumerRunnable.shutdown();

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Application has exited[3]");
        }
        ));

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Application got interrupted", e);
        } finally {
            logger.info("Application is closing[4]");
        }
    }

    public static class ConsumerRunnable implements Runnable {

        private final Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class);
        private final CountDownLatch latch;
        private final KafkaConsumer<String, String> consumer;

        ConsumerRunnable(String bootStrapServers,
                         String groupId,
                         String topic,
                         CountDownLatch latch) {
            this.latch = latch;

            Properties properties = new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            consumer = new KafkaConsumer<>(properties);
            consumer.subscribe(Collections.singletonList(topic));
        }

        @Override
        public void run() {
            try {
                while (true) {
                    ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : consumerRecords) {
                        logger.info(
                                "Received new metadata\n" +
                                        "Key: " + record.key() + "\n" +
                                        "Value: " + record.value() + "\n" +
                                        "Topic: " + record.topic() + "\n" +
                                        "Partition: " + record.partition() + "\n" +
                                        "Offset: " + record.offset() + "\n"
                        );
                    }
                }
            } catch (WakeupException e) {
                logger.info("Received shutdown signal[2]");
            } finally {
                consumer.close();
                latch.countDown(); // tell the main code we are done with the consumer
            }
        }

        public void shutdown() {
            consumer.wakeup(); // the wakeup() method is a special method to interrupt consumer.poll. it will throw the WakeUpException
        }
    }
}
