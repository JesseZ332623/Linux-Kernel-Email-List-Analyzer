-- AI 分析资源消耗审计需求表结构设计

CREATE TABLE `application_api_keys` (
  `id` 				 BIGINT       NOT NULL AUTO_INCREMENT,
  `application_name` VARCHAR(255) NOT NULL COMMENT '应用名称或账号',
  `api_key` 		 VARCHAR(255) NOT NULL COMMENT 'API 密钥',
  PRIMARY KEY (`id`)
)
ENGINE=InnoDB AUTO_INCREMENT=3
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT='第三方应用访问 API Keys 表';

CREATE TABLE `ai_model_answer_audit` (
  `id` 				   BIGINT      NOT NULL,
  `task_id` 		   CHAR(36)    NOT NULL COMMENT '本次大模型请求的唯一标识符，用于追踪和问题排查',
  `object`  		   VARCHAR(45) NOT NULL COMMENT '对象类型，表示这是一个完整的对话生成结果',
  `created` 		   BIGINT      NOT NULL COMMENT 'Unix 时间戳，表示响应的生成时间',
  `model` 			   VARCHAR(45) NOT NULL COMMENT '实际处理请求的模型名称',
  `system_fingerprint` CHAR(45)    NOT NULL COMMENT '后端环境标识，表示处理该请求的具体系统版本 / 配置，调试用',
  `create_at` 		   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审计记录创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`) USING BTREE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT='AI 模型 LKML 分析任务响应审计表';

CREATE TABLE `ai_model_answer_content` (
  `id` 				  BIGINT       NOT NULL,
  `task_id` 		  CHAR(36)     NOT NULL COMMENT '本次大模型请求的唯一标识符，用于追踪和问题排查',
  `reasoning_content` MEDIUMTEXT   COMMENT 'AI 推理文本',
  `content` 		  MEDIUMTEXT   COMMENT 'AI 输出文本',
  `create_at` 	      DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`) USING BTREE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
ROW_FORMAT=COMPRESSED
KEY_BLOCK_SIZE=8
COMMENT='AI 模型回复内容表';

CREATE TABLE `ai_model_answer_usage` (
  `id` 			  	          BIGINT   NOT NULL COMMENT '本次大模型请求的唯一标识符，用于追踪和问题排查',
  `task_id` 	  	          CHAR(36) NOT NULL COMMENT '输入提示词消耗的 Token 数',
  `prompt_tokens`             BIGINT   NOT NULL COMMENT '输入提示词消耗的 Token 数',
  `completion_tokens`         BIGINT   NOT NULL COMMENT 'AI 模型输出消耗的 Token 数（推理消耗 + 最终输出的消耗）',
  `total_tokens`          	  BIGINT   NOT NULL COMMENT '总消耗 Token 数',
  `prompt_tokens_details`     JSON     DEFAULT NULL COMMENT '输入 Token 的详细构成 JSON',
  `completion_tokens_details` JSON     DEFAULT NULL COMMENT '输出 Token 的详细构成 JSON',
  `prompt_cache_hit_tokens`   BIGINT   NOT NULL COMMENT '输入提示词缓存命中 Token 数',
  `prompt_cache_miss_tokens`  BIGINT   NOT NULL COMMENT '输入提示词缓存未命中 Token 数',
  `create_at` 				  DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`) USING BTREE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT='AI 模型 LKML 分析任务 Token 消耗明细表';

CREATE TABLE `ai_model_daily_billing` (
  `id` 				BIGINT      NOT NULL,
  `billing_date` 	DATE        NOT NULL COMMENT '汇总日期',
  `model_name`      VARCHAR(45) NOT NULL COMMENT '模型名称',
  `total_prompt_cache_hit_tokens`  BIGINT NOT NULL COMMENT '当日累计命中缓存的输入 Token 数',
  `total_prompt_cache_miss_tokens` BIGINT NOT NULL COMMENT '当日累计未命中缓存的输入 Token 数',
  `total_completion_tokens`        BIGINT NOT NULL COMMENT '当日累计输出 Token 数',
  `total_cost_rmb`                 DECIMAL(12,6) NOT NULL COMMENT '当日本币总花费（保留6位小数）',
  `create_at`                      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_billing_date_model` (`billing_date`,`model_name`) USING BTREE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT='AI 模型 token 资费消耗每日汇总表';

CREATE TABLE `linux_kernal_email` (
  `id`              BIGINT       NOT NULL,
  `task_id`         CHAR(36)     NOT NULL COMMENT '本次大模型请求的唯一标识符，用于追踪和问题排查',
  `message_id`      VARCHAR(128) NOT NULL COMMENT 'RFC 822 消息 ID',
  `from`            VARCHAR(64)  NOT NULL COMMENT '邮件发送人',
  `subject`         VARCHAR(256) NOT NULL COMMENT '邮件标题 ',
  `utc_time`        VARCHAR(64)  NOT NULL COMMENT '邮件发送时间（UTC 时区）',
  `kernel_time`     VARCHAR(64)  NOT NULL COMMENT '邮件发送时间（LKML 常用时区）',
  `text_content`    MEDIUMTEXT   COMMENT '邮件正文（纯文本）',
  `create_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`)       USING BTREE,
  KEY `idx_message_id` (`message_id`) USING BTREE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
ROW_FORMAT=COMPRESSED
KEY_BLOCK_SIZE=8
COMMENT='内核邮件数据表';