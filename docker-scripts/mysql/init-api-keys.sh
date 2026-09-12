#!/bin/bash
# 在 MySQL 容器就绪后，往数据表中插入初始数据的脚本

# 1. 插入 API Keys
# 2. 插入模型定价数据
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "${MYSQL_DATABASE}" <<EOF

INSERT INTO \`application_api_keys\`(\`application_name\`, \`api_key\`) VALUES
('${API_KEY_EMAIL}', '${API_KEY_EMAIL_VALUE}'),
('${API_KEY_DEEPSEEK_ANALYZER}', '${API_KEY_DEEPSEEK_ANALYZER_VALUE}'),
('${API_KEY_DEEPSEEK_CHAT}', '${API_KEY_DEEPSEEK_CHAT_VALUE}'),
('${API_KEY_DEEPSEEK_ABSTRACT}', '${API_KEY_DEEPSEEK_ABSTRACT_VALUE}');

INSERT INTO \`lkml_analyze\`.\`ai_model_token_pricing\`
(\`model_name\`, \`prompt_cache_hit_price\`, \`prompt_cache_miss_price\`, \`completion_price\`)
VALUES
('deepseek-v4-flash', 0.02, 1.00, 2.00),
('deepseek-v4-pro', 0.025, 3.00, 6.00),
('deepseek-flash', 0.02, 1.00, 4.00);
EOF

echo "API Keys init complete."