package com.jesse.bootstrap.it;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jesse.analyze_report_discuss.components.report_cache.KernelEmailAnalyzeReportCacher;
import com.jesse.analyze_report_discuss.dto.KernelEmailAnalyzeReport;
import com.jesse.analyze_report_discuss.exception.DiscussSessionException;
import com.jesse.analyze_report_discuss.repository.AnalyzeReportDiscussSessionDetailsRepository;
import com.jesse.analyze_report_discuss.repository.AnalyzeReportDiscussSessionRepository;
import com.jesse.analyze_report_discuss.response.DeleteDiscussSessionResponse;
import com.jesse.analyze_report_discuss.service.impl.AnalyzeReportDiscussSessionServiceImpl;
import com.jesse.core.components.global_id.GlobalIdConsumer;
import com.jesse.core.entity.AnalyzeReportDiscussSession;
import com.jesse.core.entity.AnalyzeReportDiscussSessionDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** 会话建/删写路径集成测试（Testcontainers MySQL）。*/
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = SessionWritePathTest.App.class)
@TestPropertySource(properties = "mybatis-plus.type-enums-package=com.jesse.core.enums")
class SessionWritePathTest
{
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableRetry
    @MapperScan("com.jesse.**.repository")
    @Import(AnalyzeReportDiscussSessionServiceImpl.class)
    static class App {}

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("lkml_analyze")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("schema/table-init.sql");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry)
    {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @MockitoBean
    private GlobalIdConsumer globalIdConsumer;

    @MockitoBean
    private KernelEmailAnalyzeReportCacher cacher;

    @Autowired
    private AnalyzeReportDiscussSessionServiceImpl service;

    @Autowired
    private AnalyzeReportDiscussSessionRepository sessionRepository;

    @Autowired
    private AnalyzeReportDiscussSessionDetailsRepository detailsRepository;

    private KernelEmailAnalyzeReport report(String subject)
    {
        final KernelEmailAnalyzeReport report = new KernelEmailAnalyzeReport();
        report.setEmailSubject(subject);
        return report;
    }

    @Test
    @DisplayName("创建会话：插入会话记录并返回会话 UUID")
    void createNewSessionInsertsRecord()
    {
        when(this.globalIdConsumer.nextId()).thenReturn(123L);
        when(this.cacher.getOrLoad("task-1")).thenReturn(Optional.of(this.report("[PATCH] net: fix foo")));

        final UUID sessionId = this.service.createNewDiscussSession("task-1");

        assertThat(sessionId).isNotNull();

        final List<AnalyzeReportDiscussSession> sessions = this.sessionRepository.selectList(
            Wrappers.<AnalyzeReportDiscussSession>lambdaQuery()
                .eq(AnalyzeReportDiscussSession::getTaskId, "task-1")
        );

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getTitle()).contains("Discussion on analyze report");
        assertThat(sessions.get(0).getSessionId()).isEqualTo(sessionId.toString());
    }

    @Test
    @DisplayName("创建会话：报告不存在时抛 DiscussSessionException")
    void createNewSessionThrowsWhenReportMissing()
    {
        when(this.globalIdConsumer.nextId()).thenReturn(124L);
        when(this.cacher.getOrLoad("task-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.createNewDiscussSession("task-1"))
            .isInstanceOf(DiscussSessionException.class)
            .hasMessageContaining("Report not exist");
    }

    @Test
    @DisplayName("删除会话：移除会话及其下的对话明细")
    void deleteBySessionIdRemovesSessionAndDetails()
    {
        when(this.globalIdConsumer.nextId()).thenReturn(200L);
        when(this.cacher.getOrLoad("task-2")).thenReturn(Optional.of(this.report("subj")));

        final UUID sessionId = this.service.createNewDiscussSession("task-2");

        final AnalyzeReportDiscussSessionDetails detail = new AnalyzeReportDiscussSessionDetails();
        detail.setId(201L);
        detail.setSessionId(sessionId.toString());
        detail.setQuestion("question?");
        detail.setCreateAt(LocalDateTime.now());
        this.detailsRepository.insert(detail);

        final DeleteDiscussSessionResponse response
            = this.service.deleteBySessionId("task-2", List.of(sessionId.toString()));

        assertThat(response.getDeletedSessions()).isEqualTo(1);
        assertThat(response.getDeletedSessionDetails()).isEqualTo(1);

        assertThat(this.sessionRepository.selectCount(
            Wrappers.<AnalyzeReportDiscussSession>lambdaQuery()
                .eq(AnalyzeReportDiscussSession::getTaskId, "task-2")
        )).isZero();
    }
}
