package com.jesse.bootstrap.it;

import com.jesse.core.components.global_id.GlobalIdConsumer;
import com.jesse.core.utils.ZoneUtils;
import com.jesse.response_audit.components.token_calculate.ModelTokenCalculator;
import com.jesse.response_audit.repository.AIModelDailyBillingRepository;
import com.jesse.response_audit.service.impl.AIModelDailyBillingServiceImpl;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 每日结算幂等写路径集成测试（Testcontainers MySQL）。*/
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = DailyBillingWritePathTest.App.class)
@TestPropertySource(properties = "mybatis-plus.type-enums-package=com.jesse.core.enums")
class DailyBillingWritePathTest
{
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableRetry
    @MapperScan("com.jesse.**.repository")
    @Import(AIModelDailyBillingServiceImpl.class)
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
    private ModelTokenCalculator modelTokenCalculator;

    @Autowired
    private AIModelDailyBillingServiceImpl service;

    @Autowired
    private AIModelDailyBillingRepository billingRepository;

    @Test
    @DisplayName("无消耗时插入空汇总，重复结算幂等不重复插入")
    void saveInsertsEmptyBillAndIsIdempotent()
    {
        when(this.globalIdConsumer.nextId()).thenReturn(1L);

        final LocalDate yesterday = LocalDate.now(ZoneUtils.LOCAL_TIMEZONE).minusDays(1);

        final LocalDate result = this.service.save();
        assertThat(result).isEqualTo(yesterday);
        assertThat(this.billingRepository.selectCount(null)).isEqualTo(1L);

        // 重复结算：已有记录，直接返回，不再生成 ID 也不重复插入
        this.service.save();
        assertThat(this.billingRepository.selectCount(null)).isEqualTo(1L);
        verify(this.globalIdConsumer, times(1)).nextId();
    }
}
