-- 测试用表结构（拷贝自 docker-scripts/mysql/table-init.sql）
-- 设置客户端、连接、结果的字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;
SET character_set_server = utf8mb4;
SET collation_connection = utf8mb4_0900_ai_ci;
SET collation_server = utf8mb4_0900_ai_ci;

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

CREATE TABLE `ai_model_token_pricing`(
    `id`                      BIGINT         NOT NULL AUTO_INCREMENT,
    `model_name`              VARCHAR(255)   NOT NULL COMMENT '模型名称',
    `prompt_cache_hit_price`  DECIMAL(12, 6) NOT NULL COMMENT '每百万输入（缓存命中）的价格（单价：元，保留六位小数）',
    `prompt_cache_miss_price` DECIMAL(12, 6) NOT NULL COMMENT '每百万输入（缓存未命中）的价格（单价：元，保留六位小数）',
    `completion_price`        DECIMAL(12, 6) NOT NULL COMMENT '每百万输出的价格（单价：元，保留六位小数）',
    `create_at`               DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '模型定价创建时间',
    `update_at`               DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '模型定价修改时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_name` (`model_name`) USING BTREE
) ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT = '大模型 Token 定价表';

CREATE TABLE `ai_model_answer_audit` (
  `id` 				   BIGINT       NOT NULL,
  `task_id` 		   CHAR(36)     NOT NULL COMMENT '本次大模型请求的唯一标识符，用于追踪和问题排查',
  `object`  		   VARCHAR(45)  NOT NULL COMMENT '对象类型，表示这是一个完整的对话生成结果',
  `created` 		   BIGINT       NOT NULL COMMENT 'Unix 时间戳，表示响应的生成时间',
  `model` 			   VARCHAR(45)  NOT NULL COMMENT '实际处理请求的模型名称',
  `system_fingerprint` VARCHAR(255) NOT NULL COMMENT '后端环境标识，表示处理该请求的具体系统版本 / 配置，调试用',
  `create_at` 		   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审计记录创建时间',
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
  `id` 			  	          BIGINT   NOT NULL,
  `task_id` 	  	          CHAR(36) NOT NULL,
  `prompt_tokens`             BIGINT   NOT NULL,
  `completion_tokens`         BIGINT   NOT NULL,
  `total_tokens`          	  BIGINT   NOT NULL,
  `prompt_tokens_details`     JSON     DEFAULT NULL,
  `completion_tokens_details` JSON     DEFAULT NULL,
  `prompt_cache_hit_tokens`   BIGINT   NOT NULL,
  `prompt_cache_miss_tokens`  BIGINT   NOT NULL,
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
  `model_name`      VARCHAR(45) DEFAULT NULL COMMENT '模型名称',
  `total_prompt_cache_hit_tokens`  BIGINT NOT NULL DEFAULT 0,
  `total_prompt_cache_miss_tokens` BIGINT NOT NULL DEFAULT 0,
  `total_completion_tokens`        BIGINT NOT NULL DEFAULT 0,
  `total_cost_rmb`                 DECIMAL(12,6) NOT NULL DEFAULT 0.000000,
  `create_at`                      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_billing_date_model` (`billing_date`,`model_name`) USING BTREE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT='AI 模型 token 资费消耗每日汇总表';

CREATE TABLE `linux_kernel_email` (
  `id`              BIGINT       NOT NULL,
  `task_id`         CHAR(36)     NOT NULL,
  `status`  	    TINYINT      NOT NULL DEFAULT '0',
  `message_id`      VARCHAR(128) NOT NULL,
  `from`            VARCHAR(128) NOT NULL,
  `subject`         VARCHAR(256) NOT NULL,
  `utc_time`        VARCHAR(64)  NOT NULL,
  `kernel_time`     VARCHAR(64)  NOT NULL,
  `text_content`    MEDIUMTEXT,
  `version`			INT			 NOT NULL DEFAULT 0,
  `create_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
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

CREATE TABLE `ai_analyze_discuss_session` (
  `id` 		   BIGINT NOT NULL,
  `session_id` CHAR(36) NOT NULL,
  `task_id`    CHAR(36) NOT NULL,
  `title` 	   MEDIUMTEXT NOT NULL,
  `create_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `session_id_UNIQUE` (`session_id`)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT='Linux 内核邮件分析报告疑惑解答会话表';

CREATE TABLE `ai_analyze_discuss_session_details` (
  `id` 	       	      BIGINT   NOT NULL,
  `session_id` 	      CHAR(36) NOT NULL,
  `question`          MEDIUMTEXT,
  `model_response_id` CHAR(36) DEFAULT NULL,
  `create_at` 	      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `model_response_id_UNIQUE` (`model_response_id`),
  KEY `session_id_idx` (`session_id`) USING BTREE
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_0900_ai_ci
COMMENT='Linux 内核邮件分析报告疑惑解答会话对话明细表';
