package com.example.kafka.tutorial2;

import com.example.kafka.tutorial1.ConsumerDemo;
import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterProducer {

    private Logger logger = LoggerFactory.getLogger(TwitterProducer.class);

    private static final String API_KEY = "BfM9OYK3LcY4gtne4pThwh6MZ";
    private static final String API_SECRET_KEY = "1YYl3rQDpL7W6HKm00KYGxaMxo91gHToLRKzI6WQYrjbvQAurj";
    private static final String ACCESS_TOKEN_SECRET = "dvAZdGHRTRHfSwV19XL98rKdo3wnTdEVMdqfhp4aOLoAF";
    private final String ACCESS_TOKEN = "171864751-JMesbGFAMeX6mgu8WzqrmKIXulTagm9ZDp8qOgve";

    public static void main(String[] args) {
        new TwitterProducer().launch();
    }

    private void launch() {
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);
        Client hosebirdClient =createTwitterClient(msgQueue);
        // Attempts to establish a connection.
        hosebirdClient.connect();

        // on a different thread, or multiple different threads....
        while (!hosebirdClient.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                hosebirdClient.stop();
            }
            if(msg != null){
               logger.info(msg);
            }
        }
    }

    private Client createTwitterClient(BlockingQueue<String> msgQueue) {
        /** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */


        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        // Optional: set up some followings and track terms
        List<String> terms = Lists.newArrayList("bitcoin");
        hosebirdEndpoint.trackTerms(terms);

        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(API_KEY, API_SECRET_KEY, ACCESS_TOKEN, ACCESS_TOKEN_SECRET);

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01") // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));

        Client hosebirdClient = builder.build();
        return hosebirdClient;
    }


}
