package com.jesse.bootstrap;

import com.jesse.response_audit.controller.AIModelDailyBillingController;
import com.jesse.response_audit.service.AIModelDailyBillingService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@link AIModelDailyBillingController} 幂等结算端点 Web 层测试。*/
@SpringBootTest(classes = AIModelDailyBillingControllerTest.App.class)
@AutoConfigureMockMvc
class AIModelDailyBillingControllerTest
{
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(AIModelDailyBillingController.class)
    static class App {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AIModelDailyBillingService billingService;

    @Test
    @DisplayName("POST /daily-billing 正常结算返回 200 与结算日期")
    void manualSaveReturnsOkWithBillingDate() throws Exception
    {
        when(this.billingService.save()).thenReturn(LocalDate.of(2026, 9, 17));

        this.mockMvc.perform(post("/api/ai-model-audit/daily-billing"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(
                Matchers.containsString("Save AI model daily bill of token usage complete.")
            ));
    }

    @Test
    @DisplayName("POST /daily-billing 上一轮未完成时返回 409")
    void manualSaveReturnsConflictWhenAlreadySaving() throws Exception
    {
        when(this.billingService.save())
            .thenThrow(new IllegalStateException("Previous daily billing is still saving."));

        this.mockMvc.perform(post("/api/ai-model-audit/daily-billing"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(
                "Previous daily billing is still saving."
            ));
    }
}
