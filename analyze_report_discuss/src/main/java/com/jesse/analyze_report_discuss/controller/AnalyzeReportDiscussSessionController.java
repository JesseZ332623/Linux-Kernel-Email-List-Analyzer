package com.jesse.analyze_report_discuss.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jesse.analyze_report_discuss.dto.ConversationsBySessionId;
import com.jesse.analyze_report_discuss.request.ConversationPagesRequest;
import com.jesse.analyze_report_discuss.request.DeleteSelectedSessionRequest;
import com.jesse.analyze_report_discuss.request.PaginateConversationRequest;
import com.jesse.analyze_report_discuss.response.DeleteDiscussSessionResponse;
import com.jesse.analyze_report_discuss.response.DiscussSessionResponse;
import com.jesse.core.response.PageResponse;
import com.jesse.analyze_report_discuss.service.AnalyzeReportDiscussSessionService;
import com.jesse.core.response.CustomizedResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static java.lang.String.format;

/** 内核邮件分析报告讨论会话管理控制器。*/
@Slf4j
@RestController
@RequestMapping(path = "/api/analyze-report/session")
@RequiredArgsConstructor
public class AnalyzeReportDiscussSessionController
{
    /** 匹配标准 UUID 的正则 */
    private final static
    Pattern UUID_BRACE_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$"
    );

    /** Linux 内核邮件分析报告疑惑解答会话表服务接口。*/
    private final
    AnalyzeReportDiscussSessionService analyzeReportDiscussSessionService;

    /** 查询一篇分析报告下所有的会话记录。*/
    @GetMapping("/query")
    public CustomizedResponse<List<DiscussSessionResponse>>
    getDiscussSessionByTaskId(
        @RequestParam
        final String taskId,
        final HttpServletResponse response
    )
    {
        final List<DiscussSessionResponse> sessions
            = this.analyzeReportDiscussSessionService
                  .getDiscussSessionByTaskId(taskId);

        return
        CustomizedResponse.responseOf(
            response, HttpStatus.OK,
            format("%d sessions queried.", sessions.size()),
            sessions
        );
    }

    /** 查询一个会话下指定页大小时的页数。*/
    @GetMapping(path = "/query-conversation-pages")
    public CustomizedResponse<Long>
    getConversationPagesBySessionId(
        @RequestBody
        final ConversationPagesRequest request,
        final HttpServletResponse response
    )
    {
        try
        {
            final long pages
                = this.analyzeReportDiscussSessionService
                      .getConversationPagesBySessionId(request.getSessionId(), request.getPageSize());

            return
            CustomizedResponse.responseOf(
                response, HttpStatus.OK,
                null,
                pages
            );
        }
        catch (IllegalArgumentException exception)
        {
            return
            CustomizedResponse.responseOf(
                response, HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                null
            );
        }
    }

    /** 分页查询一个会话下所有有效的对话记录。*/
    @GetMapping(path = "query-conversations")
    public CustomizedResponse<PageResponse<ConversationsBySessionId>>
    getConversationsBySessionId(
        @RequestBody
        final PaginateConversationRequest request,
        final HttpServletResponse response
    )
    {
        try
        {
            final Page<ConversationsBySessionId> page
                = this.analyzeReportDiscussSessionService
                      .getConversationsBySessionId(
                          request.getSessionId(),
                          request.getPageNo(),
                          request.getPageSize()
                      );

            final PageResponse<ConversationsBySessionId> pageResponse
                = new PageResponse<>();

            pageResponse.setData(page.getRecords());
            pageResponse.setPageNo(page.getCurrent());
            pageResponse.setPageSize(page.getSize());
            pageResponse.setCount(page.getTotal());

            return
            CustomizedResponse.responseOf(
                response, HttpStatus.OK,
                "Query conversations from session %s"
                    .formatted(request.getSessionId()),
                pageResponse
            );
        }
        catch (IllegalArgumentException exception)
        {
            return
            CustomizedResponse.responseOf(
                response, HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                null
            );
        }
    }


    /** 在指定分析报告下创建一个新的会话。*/
    @PostMapping
    public CustomizedResponse<String> createNewDiscussSession(
        @RequestParam
        final String taskId,
        final HttpServletResponse response
    )
    {
        if (!StringUtils.hasText(taskId) || taskId.length() > 36)
        {
            return
            CustomizedResponse.responseOf(
                response, HttpStatus.BAD_REQUEST,
                "Parameter [taskId] must not be empty.",
                ""
            );
        }

        if (!UUID_BRACE_PATTERN.matcher(taskId).matches())
        {
            return
            CustomizedResponse.responseOf(
                response, HttpStatus.BAD_REQUEST,
                format("Parameter [taskId] is invalid (whitch is : %s)", taskId),
                ""
            );
        }

        final UUID sessionId
            = this.analyzeReportDiscussSessionService
                  .createNewDiscussSession(taskId);

        return
        CustomizedResponse.responseOf(
            response, HttpStatus.OK,
            "Create session success.",
            sessionId.toString()
        );
    }

    /** 删除一个分析报告下选中的会话 ID。*/
    @PostMapping("delete-selected")
    public CustomizedResponse<DeleteDiscussSessionResponse>
    deleteBySessionId(
        @RequestBody
        final DeleteSelectedSessionRequest request,
        final HttpServletResponse          response
    )
    {
        if (CollectionUtils.isEmpty(request.getSessionIds()))
        {
            return
            CustomizedResponse.responseOf(
                response, HttpStatus.BAD_REQUEST,
                "No session selected.",
                DeleteDiscussSessionResponse.EMPTY_INSTANCE
            );
        }

        final DeleteDiscussSessionResponse deleteDiscussSessionResponse
            = this.analyzeReportDiscussSessionService
                  .deleteBySessionId(request.getTaskId(), request.getSessionIds());

        return
        CustomizedResponse.responseOf(
            response, HttpStatus.OK,
            format(
                "Delete %d sessions from task id: %s complete.",
                request.getSessionIds().size(),
                request.getTaskId()
            ),
            deleteDiscussSessionResponse
        );
    }

    /** 删除一个分析报告下所有的会话 ID。*/
    @DeleteMapping("/delete-all")
    public CustomizedResponse<DeleteDiscussSessionResponse>
    deleteByTaskId(
        @RequestParam final String taskId,
        final HttpServletResponse  response
    )
    {
        final DeleteDiscussSessionResponse deleteDiscussSessionResponse
            = this.analyzeReportDiscussSessionService
                  .deleteByTaskId(taskId);

        return
        CustomizedResponse.responseOf(
            response, HttpStatus.OK,
            format("Delete all sessions from task id: %s complete.", taskId),
            deleteDiscussSessionResponse
        );
    }
}