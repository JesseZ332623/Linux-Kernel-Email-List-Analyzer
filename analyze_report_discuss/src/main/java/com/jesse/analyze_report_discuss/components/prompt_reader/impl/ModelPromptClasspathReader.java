package com.jesse.analyze_report_discuss.components.prompt_reader.impl;

import com.jesse.analyze_report_discuss.components.prompt_reader.ModelPromptReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** 模型提示词读取器（从 classpath 中读取）。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class ModelPromptClasspathReader implements ModelPromptReader
{
    /** Spring 封装的资源加载器。*/
    private final ResourceLoader resourceLoader;

    /** 从某个路径中读取提示词文本。*/
    @Override
    public Optional<String> read(String classpath)
    {
        try
        {
            return Optional.of(
                this.resourceLoader
                    .getResource(classpath)
                    .getContentAsString(StandardCharsets.UTF_8)
            );
        }
        catch (IOException exception)
        {
            log.error(
                "Prompt file not exist in classpath: {}, return empty option.",
                classpath, exception
            );

            return Optional.empty();
        }
    }
}
