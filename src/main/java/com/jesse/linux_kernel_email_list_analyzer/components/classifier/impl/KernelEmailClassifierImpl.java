package com.jesse.linux_kernel_email_list_analyzer.components.classifier.impl;

import com.jesse.linux_kernel_email_list_analyzer.components.classifier.KernelEmailClassifier;
import com.jesse.linux_kernel_email_list_analyzer.components.classifier.KernelEmailTag;
import com.jesse.linux_kernel_email_list_analyzer.utils.RegexUtils;
import jakarta.mail.internet.MimeUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import static com.jesse.linux_kernel_email_list_analyzer.components.classifier.KernelEmailTag.*;
import static com.jesse.linux_kernel_email_list_analyzer.utils.KernelEmailClassifierUtils.*;

/** 内核邮件分类器实现类。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class KernelEmailClassifierImpl implements KernelEmailClassifier
{
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
    private static KernelEmailTag
    extractEventTypeBySubject(String subject)
    {
        if (Objects.isNull(subject)) {
            throw new IllegalArgumentException("Argument <subject> must not be null.");
        }

        final Matcher matcher
            = TAG_BLOCK_PATTERN.matcher(subject);

        /*
         * 如果这封邮件标题不是完全符合标准的，
         * 则把他代入标题降级分类正则表进一步匹配，
         * 如果还是找不到则返回 UNKNOW，确保不丢件。
         */
        if (!matcher.find())
        {
            log.warn(
                "Subject: {} is not a stanard LKML, " +
                "try matching with fallback patterns.",
                subject
            );

            return
            FALLBACK_PATTERNS.entrySet().stream()
                .filter((entry) ->
                    entry.getKey().matcher(subject).find())
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(UNKNOWN);
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
    private static String extractFrom(String from)
    {
        if (Objects.isNull(from)) {
            throw new IllegalArgumentException("Argument <from> must not be null.");
        }

        final Matcher fromMatcher
            = FROM_PATTERN.matcher(decodeMimeHeader(from).trim());

        if (fromMatcher.find())
        {
            final String email
                = Objects.nonNull(fromMatcher.group(1))
                    ? fromMatcher.group(1)
                    : fromMatcher.group(2);

            // 剔除非法字符且把连续的空白字符都替换成 '-'
            return
            email.replaceAll(RegexUtils.ILLEGAL_CHARACTOR_PATTERN.pattern(), "")
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