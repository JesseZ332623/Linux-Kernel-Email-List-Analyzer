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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/** 内核邮件分析报告讨论会话、对话记录管理控制器。*/
@Slf4j
@RestController
@RequestMapping(path = "/api/analyze-report/session")
@RequiredArgsConstructor
@Tag(name = "内核邮件分析报告讨论会话、对话记录管理")
public class AnalyzeReportDiscussSessionController
{
    /** 匹配标准 UUID 的正则 */
    private final static
    Pattern UUID_BRACE_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$"
    );

    /** 控制器实例响应文档相对路径。*/
    private static final
    String SPRINGDOC_EXAMPLES_PATH = "/springdoc-examples/responses/analyze_report_discuss/";

    /** Linux 内核邮件分析报告疑惑解答会话表服务接口。*/
    private final
    AnalyzeReportDiscussSessionService analyzeReportDiscussSessionService;

    /** 查询一篇分析报告下所有的会话记录。*/
    @GetMapping("/query")
    @Operation(summary = "查询一篇分析报告下所有的会话记录")
    @ApiResponse(
        responseCode = "200",
        description  = "成功",
        content      = @Content(
            mediaType = "application/json",
            schema    = @Schema(implementation = CustomizedResponse.class),
            examples  = @ExampleObject(
                name  = "分析报告 ID：fa97331d-6557-42ca-a222-a2dd31bc6d5e 下的所有会话记录",
                externalValue = SPRINGDOC_EXAMPLES_PATH + "get-discuss-session-by-task-id-200.json"
            )
        )
    )
    public CustomizedResponse<List<DiscussSessionResponse>>
    getDiscussSessionByTaskId(
        @RequestParam
        @Parameter(
            description = "分析报告 ID",
            example     = "fa97331d-6557-42ca-a222-a2dd31bc6d5e",
            required    = true
        )
        final String taskId,

        @Parameter(hidden = true)
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
    @Operation(summary = "查询一个会话下指定页大小时的页数")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "成功",
            content      = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = CustomizedResponse.class),
                examples  = @ExampleObject(
                    name = "会话 70462e7e-7269-4f03-9f72-81b04ae5c0a4 下在每页 5 条下的对话记录页数",
                    externalValue = SPRINGDOC_EXAMPLES_PATH + "get-conversation-pages-by-session-id-200.json"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description  = "传入的每页数据条数小于 0",
            content      = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = CustomizedResponse.class),
                examples  = @ExampleObject(
                    name = "传入的每页数据条数小于 0 时的错误响应",
                    externalValue = SPRINGDOC_EXAMPLES_PATH + "get-conversation-pages-by-session-id-400.json"
                )
            )
        )
    })
    public CustomizedResponse<Long>
    getConversationPagesBySessionId(
        @RequestBody
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "会话下指定页大小查询页数的请求体",
            required    = true,
            content     = @Content(
                mediaType = "application/json",
                schema    = @Schema(
                    implementation = ConversationPagesRequest.class
                )
            )
        )
        final ConversationPagesRequest request,

        @Parameter(hidden = true)
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
                response, HttpStatus.OK, null, pages
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
    @Operation(summary = "分页查询一个会话下所有有效的对话记录")
    public CustomizedResponse<PageResponse<ConversationsBySessionId>>
    getConversationsBySessionId(
        @RequestBody
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "分页查询会话下对话记录的请求体",
            required    = true,
            content     = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = PaginateConversationRequest.class)
            )
        )
        final PaginateConversationRequest request,

        @Parameter(hidden = true)
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
    @Operation(summary = "在指定分析报告下创建一个新的会话")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "成功",
            content = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = CustomizedResponse.class),
                examples  = @ExampleObject(
                    name = "为分析报告 fa97331d-6557-42ca-a222-a2dd31bc6d5e 创建一个新会话",
                    externalValue = SPRINGDOC_EXAMPLES_PATH + "create-new-discuss-session-200.json"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description  = "分析报告 ID 为空或者不合法",
            content = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = CustomizedResponse.class),
                examples  = @ExampleObject(
                    name = "为分析报告 123456 创建一个新会话",
                    externalValue = SPRINGDOC_EXAMPLES_PATH + "create-new-discuss-session-400.json"
                )
            )
        )
    })
    public CustomizedResponse<String> createNewDiscussSession(
        @RequestParam
        @Parameter(
            description = "分析报告 ID",
            example     = "fa97331d-6557-42ca-a222-a2dd31bc6d5e",
            required    = true
        )
        final String taskId,

        @Parameter(hidden = true)
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
                format("Parameter [taskId] is invalid (which is : %s)", taskId),
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
    @Operation(summary = "删除一个分析报告下选中的会话 ID")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "成功",
            content = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = CustomizedResponse.class),
                examples  = @ExampleObject(
                    name = "删除分析报告 fa97331d-6557-42ca-a222-a2dd31bc6d5e 下选中的几个会话",
                    externalValue = SPRINGDOC_EXAMPLES_PATH + "delete-by-session-id-200.json"
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "当没有选中任何会话时",
            content     = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = CustomizedResponse.class),
                examples  = @ExampleObject(
                    name = "当没有选中任何会话时的错误响应",
                    externalValue = SPRINGDOC_EXAMPLES_PATH + "delete-by-session-id-400.json"
                )
            )
        )
    })
    public CustomizedResponse<DeleteDiscussSessionResponse>
    deleteBySessionId(
        @RequestBody
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "删除一个分析报告下选中的会话 ID 请求体",
            required    = true,
            content     = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = DeleteSelectedSessionRequest.class)
            )
        )
        final DeleteSelectedSessionRequest request,

        @Parameter(hidden = true)
        final HttpServletResponse response
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

    /** 删除一个分析报告下所有的会话。*/
    @DeleteMapping("/delete-all")
    @Operation(summary = "删除一个分析报告下所有的会话")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "成功",
            content = @Content(
                mediaType = "application/json",
                schema    = @Schema(implementation = CustomizedResponse.class),
                examples  = @ExampleObject(
                    name = "删除分析报告 fa97331d-6557-42ca-a222-a2dd31bc6d5e 下的所有会话",
                    externalValue = SPRINGDOC_EXAMPLES_PATH + "delete-by-task-id-200.json"
                )
            )
        )
    })
    public CustomizedResponse<DeleteDiscussSessionResponse>
    deleteByTaskId(
        @RequestParam
        @Parameter(
            description = "分析报告 ID",
            example     = "fa97331d-6557-42ca-a222-a2dd31bc6d5e",
            required    = true
        )
        final String taskId,

        @Parameter(hidden = true)
        final HttpServletResponse response
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