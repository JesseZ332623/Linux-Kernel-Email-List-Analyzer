package com.jesse.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** {@link RegexUtils} 公共正则表达式行为测试。*/
class RegexUtilsTest
{
    @Test
    @DisplayName("Windows 非法文件名字符被完整剔除，合法字符保留")
    void illegalCharacterPatternStripsWindowsReservedCharacters()
    {
        final String sanitized =
            "<a>:b\"c/d\\e|f?g*h=(i)j"
                .replaceAll(RegexUtils.ILLEGAL_CHARACTER_PATTERN.pattern(), "");

        assertThat(sanitized).isEqualTo("abcdefghij");
    }

    @Test
    @DisplayName("Windows 非法文件名正则不会误伤邮件地址的常见字符")
    void illegalCharacterPatternKeepsEmailSafeCharacters()
    {
        final String email = "mhiramat@kernel.org";

        assertThat(
            email.replaceAll(RegexUtils.ILLEGAL_CHARACTER_PATTERN.pattern(), "")
        ).isEqualTo(email);
    }

    @Test
    @DisplayName("连续空白被单个正则匹配用于压缩")
    void whiteSpacePatternMatchesRunsOfWhitespace()
    {
        assertThat(
            "a   b\tc".replaceAll(RegexUtils.WHITE_SPACE.pattern(), "-")
        ).isEqualTo("a-b-c");
    }

    @Test
    @DisplayName("连续连字符被匹配用于归并")
    void continuousHyphensPatternMatchesTwoOrMoreHyphens()
    {
        assertThat(
            "a---b--c".replaceAll(RegexUtils.CONTINUOUS_HYPHENS.pattern(), "-")
        ).isEqualTo("a-b-c");
    }
}
