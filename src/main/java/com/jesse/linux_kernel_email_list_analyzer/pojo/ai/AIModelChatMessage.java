package com.jesse.linux_kernel_email_list_analyzer.pojo.ai;

import lombok.*;

/**
 * 向 AI 模型提交的对话 prompt 提示词上下文类。
 * JSON 格式示例如下：
 *
 * <pre>
 *     "messages": [
 *         {"role": "system", "content": "You are a helpful assistant."},
 *         {"role": "user", "content": "Hello!"}
 *     ]
 * </pre>
 *
 *  其中 system 角色表示系统指令，用于设定 AI 的人格、行为准则和任务边界，
 *  这会影响模型回复的语气、风格和内容范围。</br>
 *  而 user 角色则是用户输入，代表当前用户提出的问题或指令。
 */
@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class AIModelChatMessage
{
    /** 角色 */
    private String role;

    /** 提示词内容 */
    private String content;
}