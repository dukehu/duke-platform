#!/bin/bash

# Duke Platform 架构改进验证脚本
# 验证跨域配置和异常处理的统一性

echo "====== Duke Platform 架构改进验证 ======"
echo ""

ERRORS=0

# 1. 检查网关是否有CORS配置
echo "✓ 检查网关CORS配置..."
if grep -q "CorsWebFilter" duke-gateway/src/main/java/com/duke/gateway/config/CorsConfig.java 2>/dev/null; then
    echo "  ✅ 网关CORS配置存在"
else
    echo "  ❌ 网关CORS配置缺失"
    ((ERRORS++))
fi

# 2. 检查duke-auth是否删除了CORS配置
echo "✓ 检查duke-auth CORS配置已删除..."
if [ ! -f "duke-auth/src/main/java/com/duke/auth/config/CorsConfig.java" ]; then
    echo "  ✅ duke-auth CORS配置已删除"
else
    echo "  ❌ duke-auth仍有CORS配置"
    ((ERRORS++))
fi

# 3. 检查duke-knowledge-qa是否删除了CORS配置
echo "✓ 检查duke-knowledge-qa CORS配置已删除..."
if [ ! -f "duke-knowledge-qa/src/main/java/com/duke/knowledgeqa/config/CorsConfig.java" ]; then
    echo "  ✅ duke-knowledge-qa CORS配置已删除"
else
    echo "  ❌ duke-knowledge-qa仍有CORS配置"
    ((ERRORS++))
fi

# 4. 检查框架中的异常处理是否已注册
echo "✓ 检查异常处理自动装配..."
if grep -q "GlobalExceptionHandler\|SecurityExceptionHandler" \
    "duke-framework/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports" 2>/dev/null; then
    echo "  ✅ 异常处理已注册自动装配"
else
    echo "  ❌ 异常处理自动装配注册缺失"
    ((ERRORS++))
fi

# 5. 编译检查
echo ""
echo "✓ 编译验证..."

echo "  编译网关..."
if (cd duke-gateway && mvn clean compile -DskipTests -q 2>/dev/null); then
    echo "    ✅ 网关编译成功"
else
    echo "    ❌ 网关编译失败"
    ((ERRORS++))
fi

echo "  编译duke-auth..."
if (cd duke-auth && mvn clean compile -DskipTests -q 2>/dev/null); then
    echo "    ✅ duke-auth编译成功"
else
    echo "    ❌ duke-auth编译失败"
    ((ERRORS++))
fi

echo "  编译框架..."
if (cd duke-framework && mvn clean compile -DskipTests -q 2>/dev/null); then
    echo "    ✅ 框架编译成功"
else
    echo "    ❌ 框架编译失败"
    ((ERRORS++))
fi

echo ""
echo "====== 验证结果 ======"
if [ $ERRORS -eq 0 ]; then
    echo "✅ 所有架构改进验证通过！"
    exit 0
else
    echo "❌ 发现 $ERRORS 个问题，请检查"
    exit 1
fi
