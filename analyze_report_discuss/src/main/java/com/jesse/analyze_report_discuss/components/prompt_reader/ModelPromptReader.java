package com.jesse.analyze_report_discuss.components.prompt_reader;

import java.util.Optional;

/** 模型提示词读取器。*/
public interface ModelPromptReader
{
    /** 从某个路径中读取提示词文本。*/
    Optional<String> read(String path);
}