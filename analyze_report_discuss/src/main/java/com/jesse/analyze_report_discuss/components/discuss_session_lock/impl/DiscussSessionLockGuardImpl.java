package com.jesse.analyze_report_discuss.components.discuss_session_lock.impl;

import com.jesse.analyze_report_discuss.components.discuss_session_lock.DiscussSessionLockGuard;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** 讨论会话锁管理器实现。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscussSessionLockGuardImpl implements DiscussSessionLockGuard
{
    /** 锁过期时间，超过这个时间还未被使用过的锁会被清理。*/
    private static final
    Duration LOCK_TIMEOUT_MS = Duration.ofMinutes(5L);

    /** 讨论会话锁表。*/
    private final
    Map<String, LockEntry> generationFlags = new ConcurrentHashMap<>();

    /** 讨论会话锁实例。*/
    @AllArgsConstructor
    private static class LockEntry
    {
        /** 每个会话下用原子布尔类型就能确保互斥并行。*/
        @Getter
        private final AtomicBoolean locked;

        /** 本锁实例的上一次使用时间戳，在清理任务执行的时候用于比对。*/
        private volatile long lastUsedTimestamp;

        /** 生产一个新的锁实例。*/
        public static LockEntry of() {
            return new LockEntry(new AtomicBoolean(false), System.currentTimeMillis());
        }

        /** 更新锁的使用时间戳。*/
        void updateTimestamp() {
            this.lastUsedTimestamp = System.currentTimeMillis();
        }

        /** 本锁是否已经过期？ */
        public boolean
        isExpired(Duration timeout) {
            return System.currentTimeMillis() > this.lastUsedTimestamp + timeout.toMillis();
        }
    }

    /** 锁实例是否过期且未被使用？ */
    private static boolean
    isExpired(LockEntry lockEntry) {
        return lockEntry.isExpired(LOCK_TIMEOUT_MS) && !lockEntry.getLocked().get();
    }

    /** 每 1 分钟执行一次过期锁清理操作，避免锁表膨胀导致 OOM。*/
    @Scheduled(fixedDelay = 1L, timeUnit = TimeUnit.MINUTES)
    protected void cleanExpireLocks()
    {
        final int before
            = this.generationFlags.size();

        this.generationFlags.values()
            .removeIf(DiscussSessionLockGuardImpl::isExpired);

        final int removedCount
            = before - this.generationFlags.size();

        if (removedCount > 0) {
            log.debug("Cleaned {} discuss session locks.", removedCount);
        }
    }

    @Override
    public boolean tryAcquire(String sessionId)
    {
        // (1) 获取锁实例（没有则新建）
        final LockEntry lockEntry
            = this.generationFlags
                  .computeIfAbsent(sessionId, (ignore) -> LockEntry.of());

        // (2) CAS 操作翻转标志位，
        // 如果返回 false 则表示别的线程正在执行会话生成
        boolean acquired
            = lockEntry.getLocked().compareAndSet(false, true);

        // (3) 无论成功与否，都更新时间戳
        lockEntry.updateTimestamp();

        return acquired;
    }

    @Override
    public void release(String sessionId)
    {
        // (1) 获取锁实例（没有则新建）
        final LockEntry lockEntry
            = this.generationFlags.get(sessionId);

        // (2) 翻转标志位并更新使用时间
        if (Objects.nonNull(lockEntry))
        {
            lockEntry.getLocked().set(false);
            lockEntry.updateTimestamp();
        }
    }

    @Override
    public Map<String, Object> getStatus()
    {
        long total   = this.generationFlags.size();
        long locked  = 0L;
        long expired = 0L;

        for (LockEntry lockEntry : this.generationFlags.values())
        {
            if (lockEntry.getLocked().get()) {
                ++locked;
            }

            if (isExpired(lockEntry)) {
                ++expired;
            }
        }

        return Map.of(
            "Total",   total,
            "Locked",  locked,
            "Expired", expired,
            "Active Unlocked", total - locked - expired,
            "lockTimeoutMinutes", LOCK_TIMEOUT_MS.toMinutes()
        );
    }
}