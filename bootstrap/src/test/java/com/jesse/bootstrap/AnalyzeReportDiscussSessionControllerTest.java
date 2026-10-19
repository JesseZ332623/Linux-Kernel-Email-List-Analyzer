package com.jesse.bootstrap;

import com.jesse.analyze_report_discuss.controller.AnalyzeReportDiscussSessionController;
import com.jesse.analyze_report_discuss.response.DeleteDiscussSessionResponse;
import com.jesse.analyze_report_discuss.response.DiscussSessionResponse;
import com.jesse.analyze_report_discuss.service.AnalyzeReportDiscussSessionService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@link AnalyzeReportDiscussSessionController} 同步端点 Web 层测试。*/
@SpringBootTest(classes = AnalyzeReportDiscussSessionControllerTest.App.class)
@AutoConfigureMockMvc
class AnalyzeReportDiscussSessionControllerTest
{
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(AnalyzeReportDiscussSessionController.class)
    static class App {}

    private static final String TASK_ID = "fa97331d-6557-42ca-a222-a2dd31bc6d5e";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyzeReportDiscussSessionService sessionService;

    @Test
    @DisplayName("GET /query 返回该报告下的会话列表")
    void getDiscussSessionByTaskIdReturnsSessions() throws Exception
    {
        final DiscussSessionResponse session = new DiscussSessionResponse();
        session.setSessionId("70462e7e-7269-4f03-9f72-81b04ae5c0a4");
        session.setTitle("Discussion ...");

        when(this.sessionService.getDiscussSessionByTaskId(TASK_ID)).thenReturn(List.of(session));

        this.mockMvc.perform(get("/api/analyze-report/session/query").param("taskId", TASK_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("1 sessions queried."))
            .andExpect(jsonPath("$.data[0].sessionId").value("70462e7e-7269-4f03-9f72-81b04ae5c0a4"));
    }

    @Test
    @DisplayName("POST 创建会话：taskId 为空返回 400")
    void createSessionRejectsEmptyTaskId() throws Exception
    {
        this.mockMvc.perform(post("/api/analyze-report/session").param("taskId", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Parameter [taskId] must not be empty."));
    }

    @Test
    @DisplayName("POST 创建会话：taskId 不是合法 UUID 返回 400")
    void createSessionRejectsMalformedTaskId() throws Exception
    {
        this.mockMvc.perform(post("/api/analyze-report/session").param("taskId", "123456"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                org.hamcrest.Matchers.containsString("is invalid")
            ));
    }

    @Test
    @DisplayName("POST 创建会话：合法 taskId 委托服务并返回会话 UUID")
    void createSessionReturnsNewSessionId() throws Exception
    {
        final UUID sessionId = UUID.fromString("70462e7e-7269-4f03-9f72-81b04ae5c0a4");
        when(this.sessionService.createNewDiscussSession(TASK_ID)).thenReturn(sessionId);

        this.mockMvc.perform(post("/api/analyze-report/session").param("taskId", TASK_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(sessionId.toString()));

        verify(this.sessionService).createNewDiscussSession(TASK_ID);
    }

    @Test
    @DisplayName("POST delete-selected：未选中任何会话返回 400")
    void deleteSelectedRejectsEmptySelection() throws Exception
    {
        this.mockMvc.perform(post("/api/analyze-report/session/delete-selected")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":\"" + TASK_ID + "\",\"sessionIds\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("No session selected."));
    }

    @Test
    @DisplayName("POST delete-selected：委托服务删除并返回结果")
    void deleteSelectedDelegatesToService() throws Exception
    {
        when(this.sessionService.deleteBySessionId(
            anyString(), anyList()
        )).thenReturn(new DeleteDiscussSessionResponse(2, 5, Map.of()));

        this.mockMvc.perform(post("/api/analyze-report/session/delete-selected")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"taskId\":\"" + TASK_ID + "\",\"sessionIds\":[\"a\",\"b\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.deletedSessions").value(2));
    }

    @Test
    @DisplayName("DELETE /delete-all 委托服务删除全部会话")
    void deleteAllDelegatesToService() throws Exception
    {
        when(this.sessionService.deleteByTaskId(TASK_ID))
            .thenReturn(new DeleteDiscussSessionResponse(3, 8, Map.of()));

        this.mockMvc.perform(delete("/api/analyze-report/session/delete-all").param("taskId", TASK_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.deletedSessions").value(3));
    }

    @Test
    @DisplayName("GET query-conversation-pages：pageSize 非法时返回 400")
    void conversationPagesRejectsNonPositivePageSize() throws Exception
    {
        when(this.sessionService.getConversationPagesBySessionId(anyString(), anyLong()))
            .thenThrow(new IllegalArgumentException("Param pageSize must large then 0"));

        this.mockMvc.perform(get("/api/analyze-report/session/query-conversation-pages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":\"sid\",\"pageSize\":0}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET query-conversation-pages：正常返回页数")
    void conversationPagesReturnsPageCount() throws Exception
    {
        when(this.sessionService.getConversationPagesBySessionId("sid", 5L)).thenReturn(3L);

        this.mockMvc.perform(get("/api/analyze-report/session/query-conversation-pages")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":\"sid\",\"pageSize\":5}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    @DisplayName("GET query-conversations：分页参数非法时返回 400")
    void conversationsRejectsInvalidPageParam() throws Exception
    {
        when(this.sessionService.getConversationsBySessionId(anyString(), anyLong(), anyLong()))
            .thenThrow(new IllegalArgumentException("Invalid page param"));

        this.mockMvc.perform(get("/api/analyze-report/session/query-conversations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionId\":\"sid\",\"pageNo\":0,\"pageSize\":5}"))
            .andExpect(status().isBadRequest());
    }
}
