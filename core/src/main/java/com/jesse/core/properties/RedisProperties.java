package com.jesse.core.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Redis 服务端属性配置类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties
{
    private String host;

    private Integer port;

    private String username;

    private String password;

    private Integer database;
}