package com.jesse.analyzer.components.classifier.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@link KernelEmailClassifierImpl} 邮件分类规则测试。*/
class KernelEmailClassifierImplTest
{
    private final KernelEmailClassifierImpl classifier = new KernelEmailClassifierImpl();

    private Path classify(String from, String subject) {
        return this.classifier.classify(from, subject);
    }

    @Test
    @DisplayName("[PATCH] 标签归档到 patch 目录")
    void patchSubjectIsArchivedUnderPatch()
    {
        final Path path = this.classify(
            "Masami Hiramatsu <mhiramat@kernel.org>",
            "[PATCH] net: stmmac: Fix DMA buffer overflow"
        );

        assertThat(path).isEqualTo(Path.of("mhiramat@kernel.org", "patch"));
    }

    @Test
    @DisplayName("[RFC PATCH] 标签归档到 rfc-patch 目录")
    void rfcPatchSubjectIsArchivedUnderRfcPatch()
    {
        final Path path = this.classify(
            "Alice <alice@kernel.org>",
            "[RFC PATCH 0/5] mm: Introduce async memory compaction"
        );

        assertThat(path).isEqualTo(Path.of("alice@kernel.org", "rfc-patch"));
    }

    @Test
    @DisplayName("[RFC] 标签归档到 rfc 目录")
    void rfcSubjectIsArchivedUnderRfc()
    {
        final Path path = this.classify(
            "Bob <bob@kernel.org>",
            "[RFC] Moving to Rust for kernel device drivers"
        );

        assertThat(path).isEqualTo(Path.of("bob@kernel.org", "rfc"));
    }

    @Test
    @DisplayName("[GIT PULL] 标签归档到 git-pull 目录")
    void gitPullSubjectIsArchivedUnderGitPull()
    {
        final Path path = this.classify(
            "Linus <torvalds@linux-foundation.org>",
            "[GIT PULL] Please pull networking fixes for 6.10-rc3"
        );

        assertThat(path).isEqualTo(Path.of("torvalds@linux-foundation.org", "git-pull"));
    }

    @Test
    @DisplayName("[ANNOUNCE] 标签归档到 announce 目录")
    void announceSubjectIsArchivedUnderAnnounce()
    {
        final Path path = this.classify(
            "Greg <gregkh@linuxfoundation.org>",
            "[ANNOUNCE] Linux 6.10-rc1"
        );

        assertThat(path).isEqualTo(Path.of("gregkh@linuxfoundation.org", "announce"));
    }

    @Test
    @DisplayName("Linus 风格的无标签发版标题降级为 announce")
    void linusStyleVersionSubjectFallsBackToAnnounce()
    {
        final Path path = this.classify("torvalds@linux-foundation.org", "Linux 7.2-rc3");

        assertThat(path).isEqualTo(Path.of("torvalds@linux-foundation.org", "announce"));
    }

    @Test
    @DisplayName("非行首的 [3/6] 系列补丁降级为 patch")
    void bareSeriesPrefixFallsBackToPatch()
    {
        final Path path = this.classify("carol@kernel.org", "iommu: Add support for ACPI [3/6]");

        assertThat(path).isEqualTo(Path.of("carol@kernel.org", "patch"));
    }

    @Test
    @DisplayName("无任何标签的标题归档到 unknown")
    void unclassifiedSubjectIsArchivedUnderUnknown()
    {
        final Path path = this.classify("dave@kernel.org", "some random discussion text");

        assertThat(path).isEqualTo(Path.of("dave@kernel.org", "unknown"));
    }

    @Test
    @DisplayName("回复邮件直接归档到 replies 目录")
    void replySubjectIsArchivedUnderReplies()
    {
        final Path path = this.classify(
            "Eve <eve@kernel.org>",
            "Re: [PATCH] crypto: eip93 fix buffer overflow"
        );

        assertThat(path).isEqualTo(Path.of("eve@kernel.org", "replies"));
    }

    @Test
    @DisplayName("显示名 + 尖括号邮箱的形式提取出邮箱地址")
    void extractsEmailFromDisplayNameAndAngleBrackets()
    {
        final Path path = this.classify("Wentao Liang <vulab@iscas.ac.cn>", "[PATCH] net: fix foo");

        assertThat(path).isEqualTo(Path.of("vulab@iscas.ac.cn", "patch"));
    }

    @Test
    @DisplayName("纯邮箱地址也能兜底提取")
    void extractsBareEmailWithoutDisplayName()
    {
        final Path path = this.classify("phucduc.bui@gmail.com", "[PATCH] mm: fix bar");

        assertThat(path).isEqualTo(Path.of("phucduc.bui@gmail.com", "patch"));
    }

    @Test
    @DisplayName("邮箱中的非法字符被剔除")
    void illegalCharactersInEmailAreStripped()
    {
        final Path path = this.classify("Name <a?b@kernel.org>", "[PATCH] x86: fix baz");

        assertThat(path).isEqualTo(Path.of("ab@kernel.org", "patch"));
    }

    @Test
    @DisplayName("from 为 null 时抛出 IllegalArgumentException")
    void nullFromThrowsIllegalArgument()
    {
        assertThatThrownBy(() -> this.classify(null, "[PATCH] net: fix foo"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("from");
    }
}
