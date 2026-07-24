package com.jesse.core.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** DeepSeek 模型对话属性配置类。*/
@Data
@ToString
@EqualsAndHashCode
@Component
@ConfigurationProperties(prefix = "app.deepseek-chat")
public class DeepSeekChatProperties
{
    /** AI 模型对话端点 */
    private String modelEndpointUrl;

    /** 模型名称 */
    private String modelName;

    /** 端点访问身份凭证名 */
    private String authorizationName;

    /** 是否开启推理模式 */
    private String thinking;

    /** 模型的推理强度 */
    private String reasoningEffort;

    /** 是否流式输出 */
    private boolean stream;

    /** 系统提示词文件所在的类路径 */
    private String sysPromptsClasspath;

    /** 用户提示词文件所在的类路径 */
    private String usrPromptsClasspath;
}