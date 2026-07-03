# Linux 内核补丁分析归档服务

<p>
    <a href="https://skillicons.dev">
        <img src="https://skillicons.dev/icons?i=mysql,rabbitmq,spring,linux,gmail" alt="技术选型">
    </a>
</p>

从邮箱服务中拉取 Linux 内核补丁邮件，交给 AI 去分析后归档，回归传统的阻塞式架构 (Tomcat + V-Thread)。

## 代码速览

[单邮件服务 IMAP 连接实例管理接口实现](https://github.com/JesseZ332623/Linux-Kernal-Email-List-Analyzer/blob/main/src/main/java/com/jesse/linux_kernel_email_list_analyzer/components/impl/SingleImapConnectionImpl.java)

[Linux 内核补丁邮件推送器实现](https://github.com/JesseZ332623/Linux-Kernal-Email-List-Analyzer/blob/main/src/main/java/com/jesse/linux_kernel_email_list_analyzer/components/impl/KernelEmailPusherImpl.java)

[Linux 内核补丁邮件分析服务](https://github.com/JesseZ332623/Linux-Kernal-Email-List-Analyzer/blob/main/src/main/java/com/jesse/linux_kernel_email_list_analyzer/service/KernelEmailAnalyzerService.java)

## LICENCE

[Apache License Version 2.0](https://github.com/JesseZ332623/Linux-Kernal-Email-List-Analyzer/blob/main/LICENSE)

## 2026.07.03