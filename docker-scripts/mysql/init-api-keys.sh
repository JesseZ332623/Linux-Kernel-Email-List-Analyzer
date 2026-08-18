#!/bin/bash
# 在 MySQL 容器就绪后，往 application_api_keys 表插入隐私数据的脚本

set -e

# 等待 MySQL 就绪
until mysqladmin ping -h localhost -uroot -p"${MYSQL_ROOT_PASSWORD}" --silent; do
    echo "Waiting for MySQL start..."
    sleep 2
done

# 插入 API Keys
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" --default-character-set=utf8mb4 "${MYSQL_DATABASE}" <<EOF
INSERT INTO \`application_api_keys\`(\`application_name\`, \`api_key\`) VALUES
('${API_KEY_EMAIL}', '${API_KEY_EMAIL_VALUE}'),
('${API_KEY_DEEPSEEK_ANALYZER}', '${API_KEY_DEEPSEEK_ANALYZER_VALUE}'),
('${API_KEY_DEEPSEEK_CHAT}', '${API_KEY_DEEPSEEK_CHAT_VALUE}'),
('${API_KEY_DEEPSEEK_ABSTRACT}', '${API_KEY_DEEPSEEK_ABSTRACT_VALUE}');
EOF

echo "API Keys init complete."