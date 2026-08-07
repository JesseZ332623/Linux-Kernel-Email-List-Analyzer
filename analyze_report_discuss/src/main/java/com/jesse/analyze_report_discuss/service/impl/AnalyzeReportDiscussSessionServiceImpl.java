package com.jesse.analyze_report_discuss.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jesse.analyze_report_discuss.components.report_cache.KenelEmailAnalyzeReportCacher;
import com.jesse.analyze_report_discuss.dto.ConversationsBySessionId;
import com.jesse.analyze_report_discuss.dto.KenelEmailAnalyzeReport;
import com.jesse.analyze_report_discuss.dto.SessionCountByTaskId;
import com.jesse.analyze_report_discuss.exception.DiscussSessionException;
import com.jesse.analyze_report_discuss.repository.AnalyzeReportDiscussSessionDetailsRepository;
import com.jesse.analyze_report_discuss.repository.AnalyzeReportDiscussSessionRepository;
import com.jesse.analyze_report_discuss.response.DeleteDiscussSessionResponse;
import com.jesse.analyze_report_discuss.response.DiscussSessionResponse;
import com.jesse.analyze_report_discuss.service.AnalyzeReportDiscussSessionService;
import com.jesse.core.components.global_id.GlobalIdConsumer;
import com.jesse.core.entity.AnalyzeReportDiscussSession;
import com.jesse.core.utils.ZoneUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** Linux 内核邮件分析报告疑惑解答会话表服务实现类。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeReportDiscussSessionServiceImpl
    extends    ServiceImpl<AnalyzeReportDiscussSessionRepository, AnalyzeReportDiscussSession>
    implements AnalyzeReportDiscussSessionService
{
    /** 全局 ID 消费机接口。*/
    private final GlobalIdConsumer globalIdConsumer;

    /** 内核邮件分析报告 Redis 缓存器接口。*/
    private final
    KenelEmailAnalyzeReportCacher kenelEmailAnalyzeReportCacher;

    /** Linux 内核邮件分析报告疑惑解答会话对话内容表仓储类。*/
    private final
    AnalyzeReportDiscussSessionDetailsRepository analyzeReportDiscussSessionDetailsRepository;

    /** 每一篇报告都下维护一个标题索引。*/
    private static final
    Map<String, AtomicInteger> SESSION_INDEX_MAP = new ConcurrentHashMap<>();

    /** 在服务启动的时候初始化标题索引表。*/
    @PostConstruct
    protected void initSessionIndexMap()
    {
        List<SessionCountByTaskId> sessionCountByTaskIdList
            = this.baseMapper.getSessionCountByTaskId();

        if (!CollectionUtils.isEmpty(sessionCountByTaskIdList))
        {
            for (var sessionCountByTaskId : sessionCountByTaskIdList)
            {
                SESSION_INDEX_MAP.put(
                    sessionCountByTaskId.getTaskId(),
                    new AtomicInteger(sessionCountByTaskId.getCount() + 1)
                );
            }
        }

        log.info(
            "Initialized session index map for {} tasks",
            sessionCountByTaskIdList.size()
        );
    }

    /** 拼接会话标题。*/
    private static String
    concatSessionTitle(String taskId, String emailSubject)
    {
        final AtomicInteger sessionIndex
            = SESSION_INDEX_MAP.computeIfAbsent(
                taskId, (ignore) -> new AtomicInteger(1)
            );

        return
        "Discussion on analyze report \"%s\" - %d"
            .formatted(emailSubject, sessionIndex.getAndIncrement());
    }

    /** 查询一篇分析报告下所有的会话记录。*/
    @Override
    public List<DiscussSessionResponse>
    getDiscussSessionByTaskId(String taskId) {
        return this.baseMapper.getDiscussSessionByTaskId(taskId);
    }

    /** 查询一个会话下指定页大小时的页数。*/
    @Override
    public long
    getConversationPagesBySessionId(String sessionId, long pageSize)
    {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Param pageSize must large then 0");
        }

        final long totalConversations
            = this.baseMapper.getConversationCountsBySessionId(sessionId);

        return (totalConversations + pageSize - 1L) / pageSize;
    }

    /** 分页查询一个会话下所有有效的对话记录。*/
    @Override
    public Page<ConversationsBySessionId>
    getConversationsBySessionId(String sessionId, long pageNum, long pageSize)
    {
        if (pageNum <= 0 || pageSize < -1)
        {
            throw new
            IllegalArgumentException(
                "Invalid page param (pageNum = %s, page size = %s)"
                    .formatted(pageNum, pageSize)
            );
        }

        final long actualPageSize
            = (pageSize == -1)
                ? this.baseMapper.getConversationCountsBySessionId(sessionId)
                : pageSize;

        final Page<ConversationsBySessionId> page
            = new Page<>(pageNum, actualPageSize);

        return this.baseMapper.getConversationsBySessionId(page, sessionId);
    }

    /** 在指定分析报告下创建一个新的会话。*/
    @Override
    public UUID createNewDiscussSession(String taskId)
    {
        final long nextId    = this.globalIdConsumer.nextId();
        final UUID sessionId = UUID.randomUUID();

        final String sessionIdString = sessionId.toString();

        // 邮件的标题从缓存中拿，如果是本报告下的第一个会话，
        // 则直接触发缓存更新。
        final Optional<KenelEmailAnalyzeReport> analyzeReport
            = this.kenelEmailAnalyzeReportCacher.getOrLoad(taskId);

        final String emailSubject
            = analyzeReport.orElseThrow(() -> DiscussSessionException.make(taskId))
                .getEmailSubject();

        final AnalyzeReportDiscussSession newDiscussSession
            = new AnalyzeReportDiscussSession(
                nextId, sessionIdString, taskId,
                concatSessionTitle(taskId, emailSubject),
                LocalDateTime.now(ZoneUtils.LOCAL_TIMEZONE)
            );

        this.baseMapper.insert(newDiscussSession);

        return sessionId;
    }

    /** 删除一篇分析报告下指定的会话记录。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteDiscussSessionResponse
    deleteBySessionId(String taskId, List<String> sessionIds)
    {
        final int deleteCounts
            = this.baseMapper.deleteBySessionId(taskId, sessionIds);

        if (deleteCounts != 0)
        {
            final Map<String, Integer> sessionDetailsMap
                = sessionIds.stream()
                    .map((sessionId) -> {
                        final int deletedSessionDetails
                            = this.analyzeReportDiscussSessionDetailsRepository
                                  .deleteBySessionId(sessionId);

                        return Map.entry(sessionId, deletedSessionDetails);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            final int deletedSessionDetails
                = sessionDetailsMap.values().stream()
                    .mapToInt(i -> i)
                    .sum();

            final AtomicInteger sessionIndex
                = SESSION_INDEX_MAP.get(taskId);

            if (Objects.nonNull(sessionIndex)) {
                sessionIndex.addAndGet(-sessionIds.size());
            }

            return new
            DeleteDiscussSessionResponse(deleteCounts, deletedSessionDetails, sessionDetailsMap);
        }

        return DeleteDiscussSessionResponse.EMPTY_INSTANCE;
    }

    /** 删除一篇分析报告下所有的会话记录。*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteDiscussSessionResponse deleteByTaskId(String taskId)
    {
        final List<String> deletedSessionIds
            = this.baseMapper
                  .getDiscussSessionByTaskId(taskId)
                  .stream()
                  .map(DiscussSessionResponse::getSessionId)
                  .toList();

        final int deleteCounts
            = this.baseMapper.deleteByTaskId(taskId);

        if (deleteCounts != 0)
        {
            final Map<String, Integer> sessionDetailsMap
                = deletedSessionIds.stream()
                      .map((sessionId) -> {
                          final int deletedSessionDetails
                              = this.analyzeReportDiscussSessionDetailsRepository
                                    .deleteBySessionId(sessionId);

                          return Map.entry(sessionId, deletedSessionDetails);
                      })
                      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            final int deletedSessionDetails
                = sessionDetailsMap.values().stream()
                    .mapToInt(i -> i)
                    .sum();

            // 删除整个 taskId 的计数器
            final AtomicInteger removed
                = SESSION_INDEX_MAP.remove(taskId);

            log.debug(
                "Removed session index for taskId: {}, was at: {}",
                taskId, Objects.nonNull(removed) ? removed.get() : "null"
            );

            return new
            DeleteDiscussSessionResponse(deleteCounts, deletedSessionDetails, sessionDetailsMap);
        }

        return DeleteDiscussSessionResponse.EMPTY_INSTANCE;
    }
}