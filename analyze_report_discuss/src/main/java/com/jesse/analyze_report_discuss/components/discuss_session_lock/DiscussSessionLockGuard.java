package com.jesse.analyze_report_discuss.components.discuss_session_lock;

import java.util.Map;

/** 讨论会话锁管理器接口。*/
public interface DiscussSessionLockGuard
{
    /** 尝试获得锁。*/
    boolean tryAcquire(String sessionId);

    /** 尝试释放锁。*/
    void release(String sessionId);

    /** 获取当前管理的锁状态快照数据。*/
    Map<String, Object> getStatus();
}