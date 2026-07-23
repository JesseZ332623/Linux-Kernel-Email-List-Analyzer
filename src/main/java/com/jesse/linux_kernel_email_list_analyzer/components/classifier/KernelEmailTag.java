package com.jesse.linux_kernel_email_list_analyzer.components.classifier;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/** 内核补丁邮件事件类型标签枚举。*/
@ToString
@RequiredArgsConstructor
public enum KernelEmailTag
{
    /**
     * 子系统维护者向 Linus Torvalds（或上级维护者）发送的代码拉取请求。</br>
     * 这是补丁生命周期的最后一站。</br>
     * 当一组补丁经过社区审查、测试并最终被子系统维护者接纳后，
     * 维护者会将这些补丁整合到自己的 Git 仓库中，然后发送 GIT PULL 邮件，
     * 请求 Linus 从他的仓库拉取代码合并到主线。
     * 示例标题：
     *
     * <pre>
     * [GIT PULL] Please pull networking fixes for 6.10-rc3
     * [GIT PULL] SCSI updates for the 6.10 merge window
     * </pre>
     */
    GIT_PULL("git-pull"),

    /**
     * 内核新版本的发布公告。</br>
     *
     * 用于宣布新的内核版本发布，包括主线 -rc 版本、稳定版、以及最终的正式版。
     * 发布者通常是 Linus Torvalds、Greg Kroah-Hartman 或其他稳定版维护者。
     * 示例标题：
     *
     * <pre>
     * [ANNOUNCE] Linux 6.10-rc1
     * [ANNOUNCE] Linux 6.9.5
     * [ANNOUNCE] Linux 6.6.35 LTS
     * </pre>
     */
    ANNOUNCE("announce"),

    /**
     * 明确请求代码审查的补丁（成熟度介于 RFC 和 PATCH 之间）。</br>
     * 表面代码已经基本完成，作者认为逻辑正确，但需要更多有经验的开发者进行深入审查。
     * 它比 RFC 更接近合入主线，但仍然 “未最终定稿”。
     * 示例标题：
     *
     * <pre>
     * [REVIEW PATCH] drm/amdgpu: Fix buffer overflow in debugfs
     * [REVIEW] locking/rtmutex: Add lockdep annotations
     * </pre>
     */
    REVIEW("review"),

    /**
     * 一个同时标记为 RFC 和 PATCH 的补丁系列。</br>
     *
     * 组合了 RFC（征求设计意见）和 PATCH（包含具体代码）两种属性。
     * 作者提交了可运行的代码，但明确表示这个方案需要社区对整体设计思路进行讨论，
     * 不急于立刻合入。
     * 示例标题：
     *
     * <pre>
     * [RFC PATCH 0/5] mm: Introduce async memory compaction
     * [RFCv2 PATCH 3/6] sched/fair: Use neural network for task placement
     * </pre>
     */
    RFC_PATCH("rfc-patch"),

    /**
     * 纯征求意见的讨论邮件（不含代码或只含伪代码/原型）。</br>
     *
     * RFC 是 Request For Comments 的缩写。
     * 作者提出一个技术问题、设计思路或架构变更方案，邀请社区讨论。
     * 它通常不包含正式的补丁代码，而是一篇论述性质的邮件。
     * 示例标题：
     *
     * <pre>
     * [RFC] Moving to Rust for kernel device drivers
     * [RFC] Deprecating i_version counter in inodes
     * </pre>
     */
    RFC("rfc"),

    /**
     * 正式的代码补丁，目标是最终合入主线内核。</br>
     *
     * LKML 上最核心、数量最多的事件类型。
     * 它包含一个或多个准备合入主线内核的代码变更。
     * 示例标题：
     *
     * <pre>
     * [PATCH] net: stmmac: Fix DMA buffer overflow
     * [PATCH v3 0/7] iommu: Add support for ACPI IVRS v2
     * </pre>
     */
    PATCH("patch"),

    /** 未知事件，用于兜底。*/
    UNKNOWN("unknown");

    /**
     * 事件名，也作为分析报告目录的一段，示例：
     *
     * <pre>
     * E:\LKML-Analyze-Report\Masami-Hiramatsu-Google-mhiramat@kernel.org\{event}
     * </pre>
     */
    @Getter
    private final String event;
}