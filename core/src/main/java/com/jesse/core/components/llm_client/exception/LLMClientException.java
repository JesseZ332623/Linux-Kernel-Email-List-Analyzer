package com.jesse.core.components.llm_client.exception;

import com.jesse.core.components.llm_client.LLMClient;

/** {@link LLMClient} 中，API 调用错误最终抛出的异常。*/
public class LLMClientException extends RuntimeException
{
    public LLMClientException(String message) {
        super(message);
    }

    public LLMClientException(String message, Exception exception) {
        super(message, exception);
    }
}
