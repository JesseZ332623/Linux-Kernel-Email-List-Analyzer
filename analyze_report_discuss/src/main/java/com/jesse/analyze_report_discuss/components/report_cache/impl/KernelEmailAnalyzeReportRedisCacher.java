package com.jesse.analyze_report_discuss.components.report_cache.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.analyze_report_discuss.components.report_cache.KernelEmailAnalyzeReportCacher;
import com.jesse.analyze_report_discuss.dto.KernelEmailAnalyzeReport;
import com.jesse.analyze_report_discuss.repository.KernelEmailAnalyzeReportRepository;
import com.jesse.core.properties.AnalyzeReportCacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

/** 内核邮件分析报告 Redis 缓存器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class KernelEmailAnalyzeReportRedisCacher
    implements KernelEmailAnalyzeReportCacher
{
    /** 内核邮件分析报告仓储类。*/
    private final
    KernelEmailAnalyzeReportRepository kernelEmailAnalyzeReportRepository;

    /** 项目通用的 Redis 操作模板。*/
    @Qualifier("generic-redis-template")
    private final
    RedisTemplate<String, Object> redisTemplate;

    /** 通用 Jackson 对象映射器。*/
    private final ObjectMapper objectMapper;

    /** 内核邮件分析报告文本缓存属性类。*/
    private final
    AnalyzeReportCacheProperties cacheProperties;

    /** 可重入锁表，在缓存被击穿时使用。*/
    private final
    Map<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /** 用 task_id 拼接完整的缓存键。*/
    private String concatCacheKey(String taskId) {
        return this.cacheProperties.getAnalyzeReportKeyPrefix() + taskId;
    }

    /** 构建 “尝试移除可重入锁表中一个空闲的锁” 的操作。*/
    private BiFunction<String, ReentrantLock, ReentrantLock>
    tryRemoveLockIfIdle(ReentrantLock lock)
    {
        Objects.requireNonNull(lock, "Parameter [lock] must not be empty!");

        return
        (ignore, v) -> {
            // 如果没有任何线程持有锁且没有任何线程在本 AQS 队列等待，
            // 则视为本锁实例视为空闲
            if (v.equals(lock) && !v.isLocked() && !v.hasQueuedThreads()) {
                return null;
            }

            // 反之保留
            return v;
        };
    }

    /**
     * 由于 Redis 中缓存的 JSON 没有携带类型信息，
     * 本方法负责做安全的类型转换。
     */
    private Optional<KernelEmailAnalyzeReport>
    safeConvert(Object object)
    {
        Objects.requireNonNull(object, "Parameter [object] must not be empty!");

        if (object instanceof KernelEmailAnalyzeReport report) {
            return Optional.of(report);
        }

        return
        Optional.of(
            this.objectMapper
                .convertValue(object, KernelEmailAnalyzeReport.class)
        );
    }

    /** 若缓存被击穿，则加锁去数据库读取再重新写入缓存。*/
    private Optional<KernelEmailAnalyzeReport> load(String taskId)
    {
        final String key
            = this.concatCacheKey(taskId);

        final Duration expire
            = this.cacheProperties.getExpire();

        final long lockWaitTime
            = this.cacheProperties.getLockWaitTime().toSeconds();

        // (1) 从锁表中获取锁实例，
        // 对于同一个 key 的缓存击穿，只能有一个线程抢到锁去更新缓存
        final ReentrantLock lock
            = this.lockMap
                  .computeIfAbsent(key, (ignore) -> new ReentrantLock());

        try
        {
            // (2) 尝试获取锁
            if (lock.tryLock(lockWaitTime, TimeUnit.SECONDS))
            {
                try
                {
                    // (3) 拿到锁后进行双重检查
                    final Object analyzeReport
                        = this.redisTemplate.opsForValue().get(key);

                    if (Objects.nonNull(analyzeReport)) {
                        return this.safeConvert(analyzeReport);
                    }

                    // (4) 从数据库读取
                    final Optional<KernelEmailAnalyzeReport> loadedAnalyzeReport
                        = this.kernelEmailAnalyzeReportRepository.getReport(taskId);

                    // (5) 更新缓存后返回
                    loadedAnalyzeReport.ifPresent(
                        (report) ->
                            this.redisTemplate.opsForValue().set(key, report, expire)
                    );

                    return loadedAnalyzeReport;
                }
                finally {
                    // (6) 解锁
                    if (lock.isLocked()) {
                        lock.unlock();
                    }
                }
            }

            // 如果锁超时了，其实可以再查一次缓存，
            // 但是在目前的并发量下还不需要这样做。
            log.warn(
                "Lock waiting timeout, return empty option. (Wait time: {} seconds).",
                lockWaitTime
            );

            return Optional.empty();
        }
        catch (InterruptedException exception)
        {
            log.warn("Interrupted while waiting for lock.", exception);

            Thread.currentThread().interrupt();

            return Optional.empty();
        }
        finally
        {
            // (7) 用完锁后，如果锁空闲则令其不可到达，
            // 避免堆内存无限暴涨导致 OOM
            this.lockMap.computeIfPresent(key, this.tryRemoveLockIfIdle(lock));
        }
    }

    /** 尝试从缓存中获取分析报告数据。*/
    @Override
    public Optional<KernelEmailAnalyzeReport> getOrLoad(String taskId)
    {
        final String key
            = this.concatCacheKey(taskId);

        // (1) 尝试从缓存中获取
        final Object analyzeReport
            = this.redisTemplate.opsForValue().get(key);

        if (Objects.nonNull(analyzeReport)) {
            return this.safeConvert(analyzeReport);
        }

        // (2) 缓存过期就加锁去数据库拿并更新缓存
        return this.load(taskId);
    }
}