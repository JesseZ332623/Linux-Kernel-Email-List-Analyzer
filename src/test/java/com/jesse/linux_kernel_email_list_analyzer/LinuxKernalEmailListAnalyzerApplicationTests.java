package com.jesse.linux_kernel_email_list_analyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jesse.linux_kernel_email_list_analyzer.components.KernelEmailPusher;
import com.jesse.linux_kernel_email_list_analyzer.request.AIModelChatRequest;
import com.jesse.linux_kernel_email_list_analyzer.response.AIModelAnswerResponse;
import com.jesse.linux_kernel_email_list_analyzer.service.KernelEmailAnalyzerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 服务组件简易测试类。*/
@SpringBootTest
class LinuxKernalEmailListAnalyzerApplicationTests
{
	/** 用于输出格式化 JSON 的 Jackson 对象映射器。*/
	@Autowired
	@Qualifier(value = "pretty-object-mapper")
	private ObjectMapper mapper;

	@Autowired
	private KernelEmailAnalyzerService kernalEmailAnalyzer;

	@Autowired
	private KernelEmailPusher kernelEmailPusher;

	@Test
	public void testAIModelChatRequestMapping() throws JsonProcessingException
	{
		final String testJSON = """
			{
			     "model": "deepseek-v4-pro",
			     "messages": [
			         {"role": "system", "content": "You are a helpful assistant."},
			         {"role": "user", "content": "Hello!"}
			     ],
			     "thinking": {"type": "enabled"},
			     "reasoning_effort": "high",
			     "stream": false
			 }
		""";

		final AIModelChatRequest aiModelChatRequest
			= mapper.readValue(testJSON, AIModelChatRequest.class);

		System.out.println(aiModelChatRequest);
	}

	@Test
	public void testAIModelAnswerResponseMapping() throws JsonProcessingException
	{
		final String testJSON = """
			{
			     "id": "18e28045-db18-4d6f-9fa7-b787af3a6d09",
			     "object": "chat.completion",
			     "created": 1781592208,
			     "model": "deepseek-v4-pro",
			     "choices": [
			         {
			             "index": 0,
			             "message": {
			                 "role": "assistant",
			                 "content": "Hi there! How can I help you today?",
			                 "reasoning_content": "We are asked: \\"Hello!\\" This is a simple greeting. The assistant should respond in a friendly manner. No complex reasoning needed. Just a greeting back."
			             },
			             "logprobs": null,
			             "finish_reason": "stop"
			         }
			     ],
			     "usage": {
			         "prompt_tokens": 12,
			         "completion_tokens": 43,
			         "total_tokens": 55,
			         "prompt_tokens_details": {
			             "cached_tokens": 0
			         },
			         "completion_tokens_details": {
			             "reasoning_tokens": 32
			         },
			         "prompt_cache_hit_tokens": 0,
			         "prompt_cache_miss_tokens": 12
			     },
			     "system_fingerprint": "fp_9954b31ca7_prod0820_fp8_kvcache_20260402"
			 }
		""";

		final AIModelAnswerResponse aiModelAnswerResponse
			= mapper.readValue(testJSON, AIModelAnswerResponse.class);

		System.out.println(aiModelAnswerResponse);
	}

	@Test
	public void pushKernalEmailTest()
	{
		this.kernelEmailPusher.push();
	}
}