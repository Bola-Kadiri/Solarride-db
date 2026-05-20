package com.solarride.solarride.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean("flutterwaveWebClient")
    public WebClient flutterwaveWebClient(
            @Value("${flutterwave.base-url}") String baseUrl,
            @Value("${flutterwave.secret-key}") String secretKey) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                                .responseTimeout(Duration.ofSeconds(30))))
                .build();
    }

    @Bean("termiiWebClient")
    public WebClient termiiWebClient(
            @Value("${termii.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                                .responseTimeout(Duration.ofSeconds(15))))
                .build();
    }
}