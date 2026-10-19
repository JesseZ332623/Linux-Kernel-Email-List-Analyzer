package com.jesse.bootstrap;

import com.jesse.analyzer.components.kernel_email_pusher.KernelEmailPusher;
import com.jesse.analyzer.controller.KernelEmailPusherController;
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

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@link KernelEmailPusherController} 推送端点 Web 层测试。*/
@SpringBootTest(classes = KernelEmailPusherControllerTest.App.class)
@AutoConfigureMockMvc
class KernelEmailPusherControllerTest
{
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(KernelEmailPusherController.class)
    static class App {}

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KernelEmailPusher kernelEmailPusher;

    @Test
    @DisplayName("POST /push 触发推送器并返回 200")
    void manualPushTriggersPusherAndReturnsOk() throws Exception
    {
        this.mockMvc.perform(post("/api/analyzer/kernel-email/push"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(
                "Push kernel email complete, " +
                "please refer to the service log for specific details."
            ));

        verify(this.kernelEmailPusher).push();
    }
}
