# Linux 内核补丁分析归档服务

<div>
    <img
        src="./images/deepseek-color.png" 
        alt="deepseek" 
        width="43px" height="43px"
        style="margin-right: 4px"
    >
    <a href="https://skillicons.dev">
        <img src="https://skillicons.dev/icons?i=mysql,rabbitmq,spring,linux,gmail" alt="技术选型">
    </a>
</div>

从邮箱服务中拉取 Linux 内核补丁邮件，交给 AI 去分析后归档，回归传统的阻塞式架构 (Tomcat + V-Thread)。

## 代码速览

[单邮件服务 IMAP 连接实例管理器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/core/src/main/java/com/jesse/core/components/imap_connection/impl/SingleImapConnectionImpl.java)

[IMAP 连接实例 keep-alive 定期保活组件](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/core/src/main/java/com/jesse/core/components/imap_connection/impl/ImapConnectionKeepAlive.java)

[内核邮件分类器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/classifier/impl/KernelEmailClassifierImpl.java)

[Linux 内核补丁邮件推送器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/kernel_email_pusher/impl/KernelEmailPusherImpl.java)

[内核邮件 -> DeepSeek 模型分析器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/kernel_email_analyzer/impl/KernelEmailDeepSeekAnalyzer.java)

[LKML 内核补丁邮件分析报告生成器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/analyze_report_generator/impl/LKMLAnalyzeTemplateGeneratorImpl.java)

[LKML 内核补丁邮件分析结果持久化器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/report_persistence/impl/LKMLAnalyzeReportWriterImpl.java)

[Linux 内核补丁邮件分析服务](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/service/impl/KernelEmailAnalyzerServiceImpl.java)

[TimeMonitor 计时器切面实现](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/core/src/main/java/com/jesse/core/components/aspect/TimeMonitorAspect.java)

内核邮件服务内状态流转实现：

  - [表示一封内核补丁邮件在本服务所有状态的枚举](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/state_machine/KernelEmailStatus.java)
  
  - [内核邮件状态机实现](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/state_machine/impl/KernelEmailStateMachineImpl.javahttps://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/analyzer/src/main/java/com/jesse/analyzer/components/state_machine/impl/KernelEmailStateMachineImpl.java) 

## LICENCE

[Apache License Version 2.0](https://github.com/JesseZ332623/Linux-Kernal-Email-List-Analyzer/blob/main/LICENSE)

## 2026.07.27
