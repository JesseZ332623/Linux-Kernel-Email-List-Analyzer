# Linux 内核补丁分析归档服务

<p>
    <a href="https://skillicons.dev">
        <img src="https://skillicons.dev/icons?i=mysql,rabbitmq,spring,linux,gmail" alt="技术选型">
    </a>
</p>

从邮箱服务中拉取 Linux 内核补丁邮件，交给 AI 去分析后归档，回归传统的阻塞式架构 (Tomcat + V-Thread)。

## 代码速览

[单邮件服务 IMAP 连接实例管理器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/src/main/java/com/jesse/linux_kernel_email_list_analyzer/components/imap_connection/impl/SingleImapConnectionImpl.java)

[IMAP 连接实例 keep-alive 定期保活组件](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/src/main/java/com/jesse/linux_kernel_email_list_analyzer/components/imap_connection/impl/ImapConnectionKeepAlive.java)

[内核邮件分类器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/src/main/java/com/jesse/linux_kernel_email_list_analyzer/components/classifier/impl/KernelEmailClassifierImpl.javahttps://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/src/main/java/com/jesse/linux_kernel_email_list_analyzer/components/classifier/impl/KernelEmailClassifierImpl.java)

[Linux 内核补丁邮件推送器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/src/main/java/com/jesse/linux_kernel_email_list_analyzer/components/impl/KernelEmailPusherImpl.java)

[内核邮件 -> DeepSeek 模型分析器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/src/main/java/com/jesse/linux_kernel_email_list_analyzer/components/impl/KernelEmailDeepSeekAnalyzer.javahttps://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/src/main/java/com/jesse/linux_kernel_email_list_analyzer/components/impl/KernelEmailDeepSeekAnalyzer.java)

[LKML 内核补丁邮件分析结果生成器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/src/main/java/com/jesse/linux_kernel_email_list_analyzer/components/impl/LKMLAnalyzeTemplateGeneratorImpl.java)

[LKML 内核补丁邮件分析结果持久化器](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/src/main/java/com/jesse/linux_kernel_email_list_analyzer/components/impl/LKMLAnalyzeReportWriterImpl.java)

[Linux 内核补丁邮件分析服务](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/src/main/java/com/jesse/linux_kernel_email_list_analyzer/service/KernelEmailAnalyzerService.java)

[TimeMonitor 计时器切面实现](https://github.com/JesseZ332623/Linux-Kernel-Email-List-Analyzer/blob/develop/src/main/java/com/jesse/linux_kernel_email_list_analyzer/components/aspect/TimeMonitorAspect.java)

## LICENCE

[Apache License Version 2.0](https://github.com/JesseZ332623/Linux-Kernal-Email-List-Analyzer/blob/main/LICENSE)

## 2026.07.14
