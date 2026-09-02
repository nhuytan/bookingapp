package com.example.booking;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.net.URI;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        String base = "http://0.0.0.0:" + port + "/";
        ResourceConfig config = new ResourceConfig()
                .packages("com.example.booking")
                .register(JacksonFeature.class);
        Database.init();
        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(base), config);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.shutdownNow()));
        System.out.println("Server running at " + base);
        Thread.currentThread().join();
    }
}
