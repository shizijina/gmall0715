package com.atguigu.gmall0715.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration //...xml
public class RedisConfig {
    @Value("${spring.redis.host:disabled}")
    private String host;

    @Value("${spring.redis.port:0}")
    private int port;

    @Value("${spring.redis.timeOut:10000}")
    private int timeOut;

     /*
    <bean id = "redisUtil" class="com.atguigu.gmall0311.config.RedisUtil">
    </bean>
     */
     @Bean
    public RedisUtil getRedisUtil(){
         // 表示配置文件中没有host
         if("disabled".equals(host)){
             return null;
         }

         RedisUtil redisUtil = new RedisUtil();
         redisUtil.initJedisPool(host,port,timeOut);
         return redisUtil;
     }
}
