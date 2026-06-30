package com.jesse.linux_kernel_email_list_analyzer.components.classifier.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.classifier.KernelEmailClassifier;
import com.jesse.linux_kernel_email_list_analyzer.components.classifier.KernelEmailEventType;
import com.jesse.linux_kernel_email_list_analyzer.utils.RegexUtils;
import jakarta.mail.internet.MimeUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jesse.linux_kernel_email_list_analyzer.components.classifier.KernelEmailEventType.*;
import static java.lang.String.format;

/** 内核邮件分类器实现类。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class KernelEmailClassifierImpl implements KernelEmailClassifier
{
    /** 补丁回复邮件的标题前缀。*/
    private static final
    Set<String> REPLY_PREFIX = Set.of("RE", "Re", "re");

    /**
     * 对于一些通过别的服务转发的内核补丁邮件，比如：
     *
     * <pre>
     * {@literal Wentao Liang <vulab@iscas.ac.cn> 通过“vger.kernel.org” }
     * </pre>
     *
     * 如果拿原始的 from 直接分类的话会造成混乱，
     * 所以本正则表达式可以完整的抓取作者 + {@literal <作者邮箱>} 的信息。
     */
    private static final
    Pattern FROM_PATTERN = Pattern.compile("^(.*?<[^>]+>)");

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
    private static final
    Pattern TAG_BLOCK_PATTERN = Pattern.compile("^\\[([^]]+)]");

    /**
     * 对于非英语国家的内核开发者提交补丁邮件，
     * 邮箱标题等元数据信息需要解码 RFC 2047 解码。
     *
     * <pre>
     * tips: 以 Linus 和各个子系统核心维护者的暴脾气，
     * 是绝对不会允许邮件正文以非英文格式书写的。（笑）
     * </pre>
     */
    private static String decodeMimeHeader(String text)
    {
        try {
            return MimeUtility.decodeText(text);
        }
        catch (UnsupportedEncodingException exception)
        {
            log.warn("Failed to decode MIME header: {}", text, exception);
            return text;
        }
    }

    /** 提取内核补丁邮件的标题中的事件类型。*/
    private static KernelEmailEventType
    extractEventTypeBySubject(String subject)
    {
        if (Objects.isNull(subject)) {
            throw new IllegalArgumentException("Argument <subject> must not be null.");
        }

        final Matcher matcher
            = TAG_BLOCK_PATTERN.matcher(subject);

        if (!matcher.find())
        {
            throw new
            IllegalArgumentException(
                format("Subject: %s is not a standard LKML.", subject)
            );
        }

        final String event
            = matcher.group(1).toUpperCase(Locale.ROOT);

        // 按优先级从高到低匹配
        if (event.contains("GIT PULL"))  return GIT_PULL;
        if (event.contains("ANNOUNCE"))  return ANNOUNCE;
        if (event.contains("REVIEW"))    return REVIEW;
        if (event.contains("RFC PATCH")) return RFC_PATCH;
        if (event.contains("RFC"))       return RFC;
        if (event.contains("PATCH"))     return PATCH;

        return UNKNOWN;
    }

    /** 按 FROM_PATTERN 正确提取发件人信息（作者 + 作者邮箱）。*/
    private String extractFrom(String from)
    {
        if (Objects.isNull(from)) {
            throw new IllegalArgumentException("Argument <from> must not be null.");
        }

        final Matcher fromMatcher
            = FROM_PATTERN.matcher(decodeMimeHeader(from));

        if (fromMatcher.find())
        {
            return
            fromMatcher.group(1).trim()
                .replaceAll(RegexUtils.ILLEGAL_CHARACTOR_PATTERN.pattern(), "")
                .replaceAll("\\s+", "-");
        }

        throw new
        IllegalArgumentException("Unexpected email from: " + from);
    }

    /** 拿到内核补丁邮件的作者和邮件标题，分类后返回归档的相对目录。*/
    @Override
    public Path classify(String from, String subject)
    {
        // 回复邮件直接归档到 replies 目录，不解析事件类型
        if (REPLY_PREFIX.stream().anyMatch(subject::contains)) {
            return Path.of(extractFrom(from)).resolve("replies");
        }

        return
        Path.of(extractFrom(from))
            .resolve(extractEventTypeBySubject(subject).getEvent());
    }
}