package com.jesse.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.core.properties.RedisProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** RedisTemplate 配置类。*/
@Configuration
@RequiredArgsConstructor
public class RedisTemplateConfig
{
    private final RedisProperties redisProperties;

    /** Redis 专用的对象映射器。*/
    @Qualifier("redis-object-mapper")
    private final ObjectMapper redisObjectMapper;

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory()
    {
        // 1. 创建独立 Redis 配置
        final RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        config.setHostName(this.redisProperties.getHost());       // Redis 地址
        config.setUsername(this.redisProperties.getUsername());   // Redis 用户名
        config.setPort(this.redisProperties.getPort());           // Redis 端口
        config.setDatabase(0);                                    // 明确指定数据库

        // 密码
        config.setPassword(
            RedisPassword.of(
                this.redisProperties.getPassword()
            )
        );

        // 2. 创建客户端配置
        final LettuceClientConfiguration clientConfig
            = LettuceClientConfiguration.builder()
                .clientOptions(
                    ClientOptions.builder()
                        .autoReconnect(true)
                        // 客户端检查到 “协议错误” 时，不再执行重连操作。
                        .suspendReconnectOnProtocolFailure(true)
                        .disconnectedBehavior(
                            // 断开连接时拒绝接收命令
                            ClientOptions.DisconnectedBehavior.REJECT_COMMANDS
                        )
                        .socketOptions(
                            SocketOptions.builder()
                                .connectTimeout(Duration.ofSeconds(2L)) // 连接超时
                                .keepAlive(true) // 自动管理 TCP 连接存活
                                .build()
                        )
                        .timeoutOptions(
                            TimeoutOptions.builder()
                                .fixedTimeout(Duration.ofSeconds(1L)) // 操作超时
                                .build()
                        ).build()
                )
                .commandTimeout(Duration.ofSeconds(1L))   // 命令超时时间
                .shutdownTimeout(Duration.ofSeconds(5L))  // 关闭超时时间
                .build();

        // 3. 创建连接工厂
        return new
        LettuceConnectionFactory(config, clientConfig);
    }

    /** 通用 RedisTemplate 配置。*/
    @Primary
    @Bean(name = "generic-redis-template")
    public RedisTemplate<String, Object>
    redisTemplate(RedisConnectionFactory connectionFactory)
    {
        final RedisTemplate<String, Object> redisTemplate
            = new RedisTemplate<>();

        final StringRedisSerializer keySerializer
            = new StringRedisSerializer(StandardCharsets.UTF_8);

        final Jackson2JsonRedisSerializer<Object> valueSerializer
            = new Jackson2JsonRedisSerializer<>(this.redisObjectMapper, Object.class);

        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setHashKeySerializer(keySerializer);
        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);

        return redisTemplate;
    }
}