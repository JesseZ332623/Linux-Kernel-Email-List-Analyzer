package com.jesse.core.exception;

/**
 * 自定义的数据库乐观锁异常，
 * 在抢乐观锁失败的时候抛出，用于重试。
 */
public class OptimisticLockException extends RuntimeException
{
    public OptimisticLockException(String message) {
        super(message);
    }
}