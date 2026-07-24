package com.jesse.analyzer.components.classifier;

import java.nio.file.Path;

/** 内核邮件分类器接口。*/
public interface KernelEmailClassifier
{
    /** 拿到内核补丁邮件的作者和邮件标题，分类后返回归档的相对目录。*/
    Path classify(String from, String subject);
}