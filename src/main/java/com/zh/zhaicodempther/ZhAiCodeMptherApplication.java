package com.zh.zhaicodempther;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zh.zhaicodempther.mapper")
public class ZhAiCodeMptherApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhAiCodeMptherApplication.class, args);
    }

}
