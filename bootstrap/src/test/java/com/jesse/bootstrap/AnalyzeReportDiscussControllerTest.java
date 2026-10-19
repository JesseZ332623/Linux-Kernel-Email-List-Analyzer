package com.jesse.bootstrap;

import com.jesse.analyze_report_discuss.controller.AnalyzeReportDiscussController;
import com.jesse.analyze_report_discuss.exception.DiscussException;
import com.jesse.analyze_report_discuss.request.ReportDiscussRequest;
import com.jesse.analyze_report_discuss.service.AnalyzeReportDiscussService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@link AnalyzeReportDiscussController} SSE 端点同步交接与错误路径测试。*/
@SpringBootTest(classes = AnalyzeReportDiscussControllerTest.App.class)
@AutoConfigureMockMvc
class AnalyzeReportDiscussControllerTest
{
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(AnalyzeReportDiscussController.class)
    static class App {}

    private static final String BODY = """
        {"taskId":"t-1","sessionId":"70462e7e-7269-4f03-9f72-81b04ae5c0a4","question":"q?"}
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyzeReportDiscussService discussService;

    @Test
    @DisplayName("讨论请求被交接给服务并返回 SseEmitter")
    void discussDelegatesToServiceAndReturnsEmitter() throws Exception
    {
        doAnswer(invocation -> {
            final SseEmitter emitter = invocation.getArgument(1);
            emitter.complete();
            return null;
        }).when(this.discussService).discuss(any(ReportDiscussRequest.class), any(SseEmitter.class));

        final MvcResult result = this.mockMvc.perform(post("/api/analyze-report/discuss")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(request().asyncStarted())
            .andReturn();

        this.mockMvc.perform(asyncDispatch(result))
            .andExpect(status().isOk());

        verify(this.discussService).discuss(any(ReportDiscussRequest.class), any(SseEmitter.class));
    }

    @Test
    @DisplayName("服务抛 DiscussException 时下发 error 事件")
    void discussExceptionSendsErrorEvent() throws Exception
    {
        doThrow(new DiscussException("report not exist"))
            .when(this.discussService).discuss(any(), any());

        final MvcResult result = this.mockMvc.perform(post("/api/analyze-report/discuss")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(request().asyncStarted())
            .andReturn();

        this.mockMvc.perform(asyncDispatch(result))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("Discuss Error")))
            .andExpect(content().string(Matchers.containsString("report not exist")));
    }

    @Test
    @DisplayName("服务抛未知异常时下发 Internal server error 事件")
    void unexpectedExceptionSendsInternalErrorEvent() throws Exception
    {
        doThrow(new RuntimeException("boom"))
            .when(this.discussService).discuss(any(), any());

        final MvcResult result = this.mockMvc.perform(post("/api/analyze-report/discuss")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
            .andExpect(request().asyncStarted())
            .andReturn();

        this.mockMvc.perform(asyncDispatch(result))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("Internal server error")));
    }
}
