package com.mgrtech.sponti_api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class FakeSmsBoxInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        var server = startServer();
        var baseUrl = "http://localhost:" + server.getAddress().getPort();

        TestPropertyValues.of(
                "sponti.smsbox.base-url=" + baseUrl,
                "sponti.smsbox.api-key=test-api-key",
                "sponti.smsbox.otp-text=Your verification code is {OTP}"
        ).applyTo(context.getEnvironment());

        context.getBeanFactory()
                .registerSingleton("fakeSmsBoxServerShutdown", (DisposableBean) () -> server.stop(0));
    }

    private HttpServer startServer() {
        try {
            var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/v2/otp/send", exchange -> respond(exchange, 200, """
                    {"code":10,"message":"OTP sent","number":"32468009911","datetime":"2026-09-27T00:00:00Z"}
                    """));
            server.createContext("/v2/otp/verify", exchange -> respond(exchange, 200, """
                    {"code":11,"message":"OTP verified","number":"32468009911","client_reference":null,"datetime":"2026-09-27T00:00:00Z"}
                    """));
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start fake SmsBox server", e);
        }
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        var response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(status, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
