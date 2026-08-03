package com.jesse.analyze_report_discuss.components.report_cache.impl;

import com.jesse.analyze_report_discuss.components.report_cache.AnalyzerReportDiscussAbstractCacher;
import com.jesse.core.properties.AnalyzeReportCacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/** 内核邮件分析报告讨论记录摘要 Redis 缓存器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerReportDiscussAbstractRedisCacher
    implements AnalyzerReportDiscussAbstractCacher
{
    /** 项目通用的 Redis 操作模板。*/
    @Qualifier("generic-redis-template")
    private final
    RedisTemplate<String, Object> redisTemplate;

    /** 内核邮件分析报告文本缓存属性类。*/
    private final
    AnalyzeReportCacheProperties cacheProperties;

    /** 用会话 ID 拼接完整的缓存键。*/
    private String concatCacheKey(String sessionId) {
        return this.cacheProperties.getDiscussAbstractKeyPrefix() + sessionId;
    }

    @Override
    public Optional<String> get(String sessionId)
    {
        final String key = this.concatCacheKey(sessionId);

        return Optional.ofNullable(
            (String) this.redisTemplate.opsForValue().get(key)
        );
    }

    @Override
    public void
    set(String sessionId, String abstractText)
    {
        final String key      = this.concatCacheKey(sessionId);
        final Duration expire = this.cacheProperties.getExpire();

        this.redisTemplate.opsForValue()
            .set(key, abstractText, expire);
    }
}