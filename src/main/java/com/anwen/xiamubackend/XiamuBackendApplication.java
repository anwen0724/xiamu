package com.anwen.xiamubackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.anwen.xiamubackend.mapper")
public class XiamuBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(XiamuBackendApplication.class, args);
    }

}
