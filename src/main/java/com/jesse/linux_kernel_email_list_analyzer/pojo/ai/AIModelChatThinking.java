package com.jesse.linux_kernel_email_list_analyzer.pojo.ai;

import lombok.*;

/**
 * DeepSeek 特有的推理开关选项，JSON 示例如下：
 *
 * <pre>
 *     "thinking": {"type": "enabled"}
 * </pre>
 *
 * enabled 表示打开推理模式，disabled 表示关闭推理模式。
 */
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AIModelChatThinking
{
    private String type;
}