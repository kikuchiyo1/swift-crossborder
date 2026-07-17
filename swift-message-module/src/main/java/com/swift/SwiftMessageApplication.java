package com.swift;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 报文平台启动入口 */
@SpringBootApplication
@EnableScheduling
public class SwiftMessageApplication {
    public static void main(String[] args) {
        SpringApplication.run(SwiftMessageApplication.class, args);
    }
}
