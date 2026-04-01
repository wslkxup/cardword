package com.cardword;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cardword.mapper")
public class CardwordApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardwordApplication.class, args);
    }
}
