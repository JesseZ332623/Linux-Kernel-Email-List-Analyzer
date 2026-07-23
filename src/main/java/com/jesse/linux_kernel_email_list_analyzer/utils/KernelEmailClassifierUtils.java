package com.jesse.linux_kernel_email_list_analyzer.utils;

import com.jesse.linux_kernel_email_list_analyzer.components.classifier.KernelEmailTag;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** 内核邮件分类器工具类。*/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class KernelEmailClassifierUtils
{
    /** 补丁回复邮件的标题前缀。*/
    public static final
    Set<String> REPLY_PREFIX = Set.of("RE", "Re", "re");

    /**
     * 对于一些通过别的邮箱服务转发的内核补丁邮件，比如：
     *
     * <pre>
     * {@literal Wentao Liang <vulab@iscas.ac.cn> 通过“vger.kernel.org” }
     * </pre>
     *
     * 如果拿原始的 from 直接分类的话会造成混乱，
     * 所以本正则表达式可以完整的抓取作者 + {@literal <作者邮箱>} 的信息。
     *
     * <h4>2026.06.30 修复</h4>
     * 至于 LKML 中某些不守规矩的邮件，
     * 比如 from 就是简单的 phucduc.bui@gmail.com 纯邮箱地址，
     * 正则表达式需要为这种莽撞的情况兜底，如下所示：
     *
     * <pre>
     * ^                # 从行首开始
     * (?:              # 非捕获组，整体是 "要么匹配这个，要么匹配那个"
     *      .*?         #   分支 A：非贪婪匹配显示名
     *      <           #         尖括号开始
     *        (         #         捕获组 1（尖括号里的邮箱）
     *          [^>]+   #           尖括号内的内容
     *        )         #         捕获组 1 结束
     *      >           #         尖括号结束
     *   |              #   或
     *      (           #   分支 B：捕获组 2（纯邮箱兜底）
     *        .+@.+     #         至少一个字符 + @ + 至少一个字符
     *      )           #   捕获组 2 结束
     * )                # 非捕获组结束
     * </pre>
     */
    public static final
    Pattern FROM_PATTERN = Pattern.compile("^(?:.*?<([^>]+)>|(.+@.+))");

    /**
     * 提取内核补丁邮件标题开头中 [] 内的完整字符串。
     *
     * <pre><code>
     * String subject
     *     = "[PATCH v3 net] ipv6: fib6: fix NULL deref in fib6_walk_continue() on multi-batch dump";
     *
     * Matcher matcher = TAG_BLOCK_PATTERN.matcher(subject);
     *
     * matcher.find();
     *
     * matcher.group(1); // PATCH v3 net
     * </code></pre>
     */
    public static final
    Pattern TAG_BLOCK_PATTERN = Pattern.compile("^\\[([^]]+)]");

    /**
     * 内核邮件标题降级分类正则表。
     * 对于某些标题中没有事件标签前缀的邮件（尤其是 Linus 本人的邮件），
     * 需要准备一个降级正则表，作为按标签分类失败时的降级策略。
     */
    public static final
    Map<Pattern, KernelEmailTag> FALLBACK_PATTERNS = makeFallbackPatternsMap();

    private static Pattern
    caseInsensitivePattern(String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    /**
     * 构造内核邮件标题降级分类正则表，
     * 需要使用不可变的 {@link LinkedHashMap} 确保插入顺序。
     */
    private static
    Map<Pattern, KernelEmailTag> makeFallbackPatternsMap()
    {
        final Map<Pattern, KernelEmailTag>
            fallbackPatterns = new LinkedHashMap<>();

        /*
         * 虽然按照 LKML 的标签标准，
         * 发布新版本的 Linux 内核应该在标题开头加 [ANNOUNCE] 标签，
         * 但是 Linus 本人基本上不遵守这个规矩，他的发版邮件的标题一般就是：
         *
         * Linux 7.2-rc3
         * Linux 6.10
         * Linux 5.18-rc1
         *
         * 所以这个降级正则就算专门为他准备的。
         */
        fallbackPatterns.put(
            caseInsensitivePattern("^Linux\\s+\\d+\\.\\d+"),
            KernelEmailTag.ANNOUNCE
        );

        // ANNOUNCE 变体
        fallbackPatterns.put(
            caseInsensitivePattern("(announcing|released|now available|is out)"),
            KernelEmailTag.ANNOUNCE
        );

        fallbackPatterns.put(
            caseInsensitivePattern("(Please pull|GIT PULL|git pull)"),
            KernelEmailTag.GIT_PULL
        );

        fallbackPatterns.put(
            caseInsensitivePattern("(please review|request for review|review requested)"),
            KernelEmailTag.REVIEW
        );

        fallbackPatterns.put(
            caseInsensitivePattern("^(RFC|Request for Comments)[\\s:]"),
            KernelEmailTag.RFC
        );

        /*
         * 有些补丁邮件标题可能不写 [PATHCH ...]，
         * 而是直接用 [3/6] 这样的前缀。
         */
        fallbackPatterns.put(
            caseInsensitivePattern("\\[\\d+/\\d+\\]"),
            KernelEmailTag.PATCH
        );

        return Collections.unmodifiableMap(fallbackPatterns);
    }
}