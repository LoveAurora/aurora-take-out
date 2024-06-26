package org.aurora;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement //开启注解方式的事务管理
@EnableCaching
@EnableScheduling //启用定时任务
@Slf4j
@MapperScan("org.aurora.mapper")
public class AuroraApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuroraApplication.class, args);
        log.info("server started");
    }
}
