package com.jesse.linux_kernel_email_list_analyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.linux_kernel_email_list_analyzer.components.LKMLAnalyzeReportWriter;
import com.jesse.linux_kernel_email_list_analyzer.components.LKMLAnalyzeTemplateGanerator;
import com.jesse.linux_kernel_email_list_analyzer.pojo.AnalyzeResultTemplateData;
import com.jesse.linux_kernel_email_list_analyzer.pojo.PlainTextEmail;
import com.jesse.linux_kernel_email_list_analyzer.pojo.ai.AIModelChatMessage;
import com.jesse.linux_kernel_email_list_analyzer.pojo.ai.AIModelChatThinking;
import com.jesse.linux_kernel_email_list_analyzer.repository.ApplicationApiKeysRepository;
import com.jesse.linux_kernel_email_list_analyzer.request.AIModelChatRequest;
import com.jesse.linux_kernel_email_list_analyzer.response.AIModelAnswerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.util.List;

/** Linux 内核补丁邮件分析服务。*/
@Slf4j
@Service
@RequiredArgsConstructor
public class KernelEmailAnalyzerService
{
    /** AI 模型对话端点。*/
    private static final
    String MODEL_ENDPOINT_URL = "https://api.deepseek.com/chat/completions";

    /** 模型名称。*/
    private static final String MODEL_NAME = "deepseek-v4-pro";

    /** 端点访问身份凭证名。*/
    private static final
    String AUTHORIZATION_NAME = "deepseek-lkml-analyer";

    /** 模型系统指令提示词。*/
    private static final
    String SYSTEM_ROLE_PROMPT = """
        你是一位资深的 Linux 内核专家，拥有 20 年以上的内核开发经验。你曾经是 Linux 内核的核心维护者之一，深度参与过内存管理、进程调度、文件系统等子系统的开发。
    
        在分析 Linux 内核邮件列表时，请遵循以下原则：
    
        1. **专业视角**：从内核架构、设计理念和工程实践的角度分析问题，指出技术决策背后的权衡（trade-off）。
    
        2. **精准定位**：准确识别邮件涉及的子系统（如 VFS、MM、Scheduler、Networking 等）和关键数据结构。
    
        3. **历史洞察**：能够联系内核的发展历史，说明某个补丁或讨论在内核演进中的位置和意义。
    
        4. **社区文化理解**：理解 Linux 内核社区的工作方式，包括：
           - 补丁提交规范（Signed-off-by、Reviewed-by、Acked-by 等标签的含义）
           - 维护者层级关系（子系统维护者 → 顶级维护者 → Linus）
           - 代码审查的严格程度和常见争论点
    
        5. **技术深度**：
           - 能解释 RCU、内存屏障、缓存一致性等底层概念
           - 理解用户空间与内核空间的交互
           - 关注性能、安全性和可维护性的平衡
    
        6. **表达风格**：
           - 使用精确的技术术语
           - 像 Linus 那样直接、犀利，但保持建设性
           - 需要时会引用内核源码路径和函数名来佐证观点
    
        7. **输出格式**：
           - 先给出核心结论
           - 然后展开技术分析
           - 最后说明影响范围和建议
    """;

    /** 模型用户指令提示词。*/
    private static final
    String USER_ROLE_PROMPT_PATTERN = """
        邮件具体内容如下所示：
        RFC 822 消息 ID：%s
        发送人：%s
        发送时间：%s
        标题：%s
        正文：%s
    
        现在你要做的就是先翻译这封邮件，
        然后详细分析这封内核补丁邮件，回答需要遵循 Markdown 语法，但是最外面不要加上
        ```md
        ```
        这样的代码块。
    """;

    /** 默认开启推理模式。*/
    private static final
    AIModelChatThinking THINKING_OPTION = new AIModelChatThinking("enabled");

    /** 分析任务中 AI 的系统指令是固定的。*/
    private static final
    AIModelChatMessage SYSTEM_ROLE_MESSAGE
        = new AIModelChatMessage("system", SYSTEM_ROLE_PROMPT);

    /** Spring 封装的 HTTP 客户端。*/
    private final RestTemplate restTemplate;

    /** 第三方应用访问 API Keys 表仓库类。*/
    private final
    ApplicationApiKeysRepository applicationApiKeysRepository;

    /** LKML 内核补丁邮件分析结果生成器接口。*/
    private final LKMLAnalyzeTemplateGanerator templateGanerator;

    /** LKML 内核补丁邮件分析结果持久化器接口。*/
    private final LKMLAnalyzeReportWriter reportWriter;

    /** 通用的 Jackson 对象映射器。*/
    private final ObjectMapper objectMapper;

    /** 用内核补丁邮件的内容格式化用户指令提示词。*/
    private AIModelChatMessage
    formatUserRolePrompt(PlainTextEmail kernalEmail)
    {
        return new AIModelChatMessage(
            "user",
            USER_ROLE_PROMPT_PATTERN.formatted(
                kernalEmail.getMessageId(),
                kernalEmail.getFrom(),
                kernalEmail.getKernalTime(),
                kernalEmail.getSubject(),
                kernalEmail.getTextContent()
            )
        );
    }

    /** 将内核邮件数据提交给 AI 模型分析，返回分析结果字符串。*/
    private AIModelAnswerResponse
    doAnalyzer(PlainTextEmail kernelEmail) throws JsonProcessingException
    {
        log.info("Analyzing kernel email: {}", kernelEmail.getMessageId());

        final HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.set(
            "Authorization",
            "Bearer " + this.applicationApiKeysRepository.findByAppName(AUTHORIZATION_NAME)
        );

        final AIModelChatRequest request = new AIModelChatRequest();

        request.setModel(MODEL_NAME);
        request.setMessages(List.of(SYSTEM_ROLE_MESSAGE, this.formatUserRolePrompt(kernelEmail)));
        request.setThinking(THINKING_OPTION);
        request.setReasoningEffort("high");
        request.setStream(false);

        final HttpEntity<AIModelChatRequest> httpEntity
            = new HttpEntity<>(request, httpHeaders);

        // 启动计时器
        final StopWatch stopWatch = StopWatch.createStarted();

        final String responseJSON
            = this.restTemplate
                  .postForObject(MODEL_ENDPOINT_URL, httpEntity, String.class);

        stopWatch.stop();

        log.info(
            "POST {} call took [{}] milliseconds.",
            MODEL_ENDPOINT_URL, stopWatch.getDuration().toMillis()
        );

        return
        this.objectMapper
            .readValue(responseJSON, AIModelAnswerResponse.class);
    }

    @RabbitListener(queues = "${app.rabbitmq-queue-props.lkml.queue-name}")
    public void handleKernelEmail(
        final PlainTextEmail        kernalEmail,
        final Channel               channel,
        final Message     message
    )
    {
        final long deliveryTag
            = message.getMessageProperties().getDeliveryTag();

        try
        {
            // (1) 执行分析
            final AIModelAnswerResponse response
                = this.doAnalyzer(kernalEmail);

            // (2) 生成分析报告
            final String htmlText
                = this.templateGanerator.generate(
                    new AnalyzeResultTemplateData(kernalEmail, response)
                );

            // (3) 写到本地文件中去
            this.reportWriter
                .write(kernalEmail.getSubject(), htmlText);

            // (4) 确认消息
            channel.basicAck(deliveryTag, false);
        }
        catch (Exception exception)
        {
            log.error("", exception);

            try
            {
                // 目前的做法略显粗暴，在分析过程中出现了任何错误，
                // 这封邮件都入死信队列。
                channel.basicNack(deliveryTag, false, false);
            }
            catch (IOException ioException)
            {
                // 如果不确认调用失败了（比如和队列服务的连接断开），
                // 消息会自己回到队列，不会丢失。
                log.error(
                    "Nack kernel email message failed (delivery tag: {})",
                    deliveryTag, ioException
                );
            }
        }
    }
}