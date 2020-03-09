package com.leyou.itemService;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.SpringCloudApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import tk.mybatis.spring.annotation.MapperScan;

import javax.persistence.Table;


//@SpringBootApplication
//@EnableDiscoveryClient
//@EnableCircuitBreaker
//@SpringCloudApplication
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.leyou.itemService.mapper")
public class LyItemService {
    public static void main(String[] args) {
        SpringApplication.run(LyItemService.class, args);
    }
}
