# Linux 内核补丁分析归档服务

<div>
    <img
        src="./images/deepseek-color.png" 
        alt="deepseek" 
        width="43px" height="43px"
        style="margin-right: 4px"
    >
    <a href="https://skillicons.dev">
        <img src="https://skillicons.dev/icons?i=java,spring,linux,gmail,mysql,redis,rabbitmq," alt="技术选型">
    </a>
</div>

从邮箱服务中拉取 Linux 内核补丁邮件，交给 AI 去分析后归档，
回归传统的多模块单体阻塞式架构 (Tomcat + V-Thread)。

## 模块速览

- [bootstrap 项目引导模块](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/tree/develop/bootstrap/src/main)

- [core 服务核心模块](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/tree/develop/core/src/main/java/com/jesse/core)

- [analyzer 内核补丁邮件拉取 -> 分析 -> 归档模块](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/tree/develop/analyzer/src/main/java/com/jesse/analyzer)

- [analyze_report_discuss 内核邮件分析报告讨论模块](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/tree/develop/analyze_report_discuss/src/main/java/com/jesse/analyze_report_discuss)

- [model_response_audit 模型调用审计模块](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/tree/develop/model_response_audit/src/main/java/com/jesse/response_audit)

## 代码速览

### core 模块

- [单邮件服务 IMAP 连接实例管理器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/core/src/main/java/com/jesse/core/components/imap_connection/impl/SingleImapConnectionImpl.java)

- [IMAP 连接实例 keep-alive 定期保活组件](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/core/src/main/java/com/jesse/core/components/imap_connection/impl/ImapConnectionKeepAlive.java)

- [TimeMonitor 计时器切面实现](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/core/src/main/java/com/jesse/core/components/aspect/TimeMonitorAspect.java)

- [表示一封内核补丁邮件在本服务所有状态的枚举](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/core/src/main/java/com/jesse/core/enums/KernelEmailStatus.java)

### model_response_audit 模块

- [AI 模型 LKML 分析任务响应审计表服务类实现](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/model_response_audit/src/main/java/com/jesse/response_audit/service/impl/AIModelAnswerAuditServiceImpl.java)

- [AI 模型 token 资费消耗每日汇总表服务类实现](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/model_response_audit/src/main/java/com/jesse/response_audit/service/impl/AIModelDailyBillingServiceImpl.java)

### analyzer 模块

- [内核邮件分类器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/classifier/impl/KernelEmailClassifierImpl.java)

- [Linux 内核补丁邮件推送器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/kernel_email_pusher/impl/KernelEmailPusherImpl.java)

- [内核邮件 -> DeepSeek 模型分析器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/kernel_email_analyzer/impl/KernelEmailDeepSeekAnalyzer.java)

- [LKML 内核补丁邮件分析报告生成器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/analyze_report_generator/impl/LKMLAnalyzeTemplateGeneratorImpl.java)

- [LKML 内核补丁邮件分析结果持久化器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/report_persistence/impl/LKMLAnalyzeReportWriterImpl.java)

- [Linux 内核补丁邮件分析服务](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/service/impl/KernelEmailAnalyzerServiceImpl.java)

- [内核邮件分析状态机实现](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/state_machine/impl/KernelEmailStateMachineImpl.java) 

### model_response_audit 模块

- [AI 模型 SSE 协议响应流处理回调实现](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/core/src/main/java/com/jesse/core/enums/KernelEmailStatus.java)

- [SSE 响应数据片处理器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyze_report_discuss/src/main/java/com/jesse/analyze_report_discuss/components/sse_callback/ResponseChunkHandler.java)

- [分析报告讨论上下文 DeepSeek 摘要器实现](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyze_report_discuss/src/main/java/com/jesse/analyze_report_discuss/components/discuss_abstract/impl/AnalyzeReportDiscussDeepSeekAbstractor.java)

- [内核邮件分析报告 Redis 缓存器实现](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyze_report_discuss/src/main/java/com/jesse/analyze_report_discuss/components/report_cache/impl/KenelEmailAnalyzeReportRedisCacher.java)

- [Linux 内核邮件分析报告疑惑解答会话表服务实现类](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyze_report_discuss/src/main/java/com/jesse/analyze_report_discuss/service/impl/AnalyzeReportDiscussSessionServiceImpl.java)

- [Linux 内核邮件分析报告疑惑解答会话对话内容表服务实现类](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyze_report_discuss/src/main/java/com/jesse/analyze_report_discuss/service/impl/AnalyzeReportDiscussSessionDetailsServiceImpl.java)

- [内核邮件分析报告讨论服务实现](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyze_report_discuss/src/main/java/com/jesse/analyze_report_discuss/service/impl/AnalyzeReportDiscussServiceImpl.java)

## LICENCE

[Apache License Version 2.0](https://github.com/JesseZ332623/Linux-Kernal-Email-List-Analyzer/blob/main/LICENSE)

## 2026.08.05
