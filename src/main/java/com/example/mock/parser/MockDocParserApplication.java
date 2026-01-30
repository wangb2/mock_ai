package com.example.mock.parser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MockDocParserApplication {
    public static void main(String[] args) {
        SpringApplication.run(MockDocParserApplication.class, args);
    }
}
