package com.example.elasticsearch;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class ElasticSearchConsumerKafkaBulkRequest {

    private static Logger logger = LoggerFactory.getLogger(ElasticSearchConsumerKafkaBulkRequest.class);

    private static KafkaConsumer<String, String> createKafkaConsumer(){
        String bootStrapServers = "127.0.0.1:9092";
        String groupId = "kafka-demo-elasticsearch";
        String topic = "twitter_tweets";

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "10");

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties);
        kafkaConsumer.subscribe(List.of(topic));

        return kafkaConsumer;
    }

    private static RestHighLevelClient createElasticSearchClient(){

        String hostname = "kafka-course-7221455738.eu-central-1.bonsaisearch.net";
        String username = "ykq894x6y8";
        String password = "uwbw17rqzd";

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        RestClientBuilder builder = RestClient
                .builder(new HttpHost(hostname, 443, "https"))
                .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                        return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                });

        return new RestHighLevelClient(builder);
    }

    private static String parseJsonValue(String value){
        return JsonParser.parseString(value).getAsJsonObject().get("id_str").getAsString();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        KafkaConsumer<String, String> kafkaConsumer = createKafkaConsumer();
        RestHighLevelClient elasticSearchClient = createElasticSearchClient();

        while (true) {
            ConsumerRecords<String, String> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(100));
            Integer recordCount = consumerRecords.count();
            logger.info("Received " + consumerRecords.count() + " records");

            BulkRequest bulkRequest = new BulkRequest();

            for (ConsumerRecord<String, String> record : consumerRecords) {
                String jsonString = record.value();

                try {
                    String id = parseJsonValue(jsonString); // can throw NPE if data has no field 'id_str'
                    IndexRequest indexRequest = new IndexRequest("twitter");
                    indexRequest.id(id);
                    indexRequest.source(jsonString, XContentType.JSON);
                    bulkRequest.add(indexRequest);
                } catch (NullPointerException e){
                    logger.warn("Skipping bad data: " + record.value());
                }
            }

            if (recordCount > 0) {
                BulkResponse bulkResponse = elasticSearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                logger.info("Committing offsets...");
                kafkaConsumer.commitSync();
                logger.info("Offsets have been committed");
                Thread.sleep(1000);
            }
        }
    }
}
