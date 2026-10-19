package com.jesse.bootstrap.it;

import com.jesse.analyzer.components.state_machine.KernelEmailEvents;
import com.jesse.analyzer.components.state_machine.impl.KernelEmailStateMachineImpl;
import com.jesse.analyzer.config.KernelEmailStateMachineConfig;
import com.jesse.analyzer.repository.LinuxKernelEmailRepository;
import com.jesse.core.entity.LinuxKernelEmailEntity;
import com.jesse.core.enums.KernelEmailStatus;
import com.jesse.core.exception.OptimisticLockException;
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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@link KernelEmailStateMachineImpl} 状态流转集成测试（Testcontainers MySQL）。*/
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = KernelEmailStateMachineTest.App.class)
@TestPropertySource(properties = "mybatis-plus.type-enums-package=com.jesse.core.enums")
class KernelEmailStateMachineTest
{
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableRetry
    @MapperScan("com.jesse.**.repository")
    @Import({KernelEmailStateMachineConfig.class, KernelEmailStateMachineImpl.class})
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

    @Autowired
    private KernelEmailStateMachineImpl stateMachine;

    @Autowired
    private LinuxKernelEmailRepository repository;

    private LinuxKernelEmailEntity newEmail(long id)
    {
        final LinuxKernelEmailEntity email = new LinuxKernelEmailEntity();
        email.setId(id);
        email.setTaskId("");
        email.setStatus(KernelEmailStatus.FETCHED);
        email.setMessageId("msg-" + id);
        email.setFrom("dev@kernel.org");
        email.setSubject("[PATCH] net: fix foo");
        email.setUtcTime("2026-09-17T00:00:00");
        email.setKernelTime("2026-09-17T00:00:00");
        email.setTextContent("content");
        return email;
    }

    @Test
    @DisplayName("完整成功流转：FETCHED -> ... -> REPORT_PERSISTENCE_SUCCESS 并持久化")
    void fullSuccessTransition()
    {
        final long id = 1001L;
        this.repository.insert(this.newEmail(id));

        KernelEmailStatus status = KernelEmailStatus.FETCHED;
        final KernelEmailEvents[] successEvents = {
            KernelEmailEvents.PUSH_SUCCESS,
            KernelEmailEvents.PULL_SUCCESS,
            KernelEmailEvents.START_ANALYSIS,
            KernelEmailEvents.ANALYSIS_SUCCESS,
            KernelEmailEvents.START_GENERATE,
            KernelEmailEvents.GENERATE_SUCCESS,
            KernelEmailEvents.START_PERSISTING,
            KernelEmailEvents.PERSISTENCE_SUCCESS
        };

        for (KernelEmailEvents event : successEvents) {
            status = this.stateMachine.fireEvent(id, event);
        }

        assertThat(status).isEqualTo(KernelEmailStatus.REPORT_PERSISTENCE_SUCCESS);
        assertThat(this.repository.selectStatusAndVersion(id).getStatus())
            .isEqualTo(KernelEmailStatus.REPORT_PERSISTENCE_SUCCESS);
    }

    @Test
    @DisplayName("非法流转被拒绝并抛 OptimisticLockException")
    void invalidTransitionIsRejected()
    {
        final long id = 1002L;
        this.repository.insert(this.newEmail(id));

        // FETCHED 不接受 START_ANALYSIS
        assertThatThrownBy(() ->
            this.stateMachine.fireEvent(id, KernelEmailEvents.START_ANALYSIS)
        ).isInstanceOf(OptimisticLockException.class);

        // 状态未发生改变
        assertThat(this.repository.selectStatusAndVersion(id).getStatus())
            .isEqualTo(KernelEmailStatus.FETCHED);
    }

    @Test
    @DisplayName("失败分支流转：FETCHED -> PUSH_FAILED")
    void failureBranchTransition()
    {
        final long id = 1003L;
        this.repository.insert(this.newEmail(id));

        final KernelEmailStatus status
            = this.stateMachine.fireEvent(id, KernelEmailEvents.PUSH_FAILURE);

        assertThat(status).isEqualTo(KernelEmailStatus.PUSH_FAILED);
    }
}
