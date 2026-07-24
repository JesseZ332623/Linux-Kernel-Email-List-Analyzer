package com.jesse.bootstrap;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jesse.analyzer.components.classifier.KernelEmailClassifier;
import com.jesse.analyzer.entity.LinuxKernerlEmailEntiy;
import com.jesse.analyzer.repository.LinuxKernerlEmailRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 内核邮件分类器测试类。*/
@Slf4j
@SpringBootTest
public class EmailSubjectClassifyTest
{
    @Autowired
    private KernelEmailClassifier kernelEmailClassifier;

    @Autowired
    private LinuxKernerlEmailRepository linuxKernerlEmailRepository;

    @Test
    public void classifyTest()
    {
        final Wrapper<LinuxKernerlEmailEntiy> queryCondition
            = Wrappers.<LinuxKernerlEmailEntiy>lambdaQuery()
                .select(LinuxKernerlEmailEntiy::getFrom, LinuxKernerlEmailEntiy::getSubject);

        this.linuxKernerlEmailRepository
            .selectList(queryCondition)
            .stream()
            .map((entity) ->
                this.kernelEmailClassifier.classify(entity.getFrom(), entity.getSubject()))
            .forEach(System.out::println);
    }
}
