package com.jesse.core.components.global_id;

import java.util.List;

/** 全局 ID 消费机接口。*/
public interface GlobalIdConsumer
{
    /** 获取下一个 ID */
    long nextId();

    /** 获取下一批 ID */
    List<Long> nextIds(int batchSize);
}
