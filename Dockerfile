# =====================================================================
# 迭代一 后端骨架 —— 运行镜像 Dockerfile（快速方案，默认使用）
# =====================================================================
# 文件     : Dockerfile
# 版本     : v1.1
# 日期     : 2026-10-12
# 作者     : 后端工程负责人（待填写姓名）
# 状态     : 草稿（待确认）
#
# 设计原则（重要）
# ---------------------------------------------------------------------
# **本镜像不做任何 Maven 构建**，只把宿主机已打好的 jar 复制进运行镜像。
#
# 为什么改成这样：
#   原多阶段方案在容器内执行 `mvn dependency:go-offline` + `mvn package`，
#   每次全新容器都要**重新下载全部 Maven 依赖**；叠加 macOS 下 Docker
#   跨卷 I/O 慢与 155MB 构建上下文，docker compose build 实测耗时约 38 分钟。
#   改为「宿主机打包 + 容器仅作运行环境」后，构建只需复制 jar，
#   实测降至 **秒级**（见 README-backend.md §3 与验证记录）。
#
# 使用方式
# ---------------------------------------------------------------------
#   1) 宿主机打包（本机已有 ~/.m2 缓存，通常几十秒内完成）：
#        mvn clean package -DskipTests
#      无本地 Maven 时用容器打包（依赖缓存在工作区 .mvnrepo，仅首次慢）：
#        docker run --rm -v "$PWD":/workspace -v "$PWD/.mvnrepo":/root/.m2 \
#          -w /workspace maven:3.9-eclipse-temurin-11 \
#          mvn -B -DskipTests clean package
#   2) 构建并启动：
#        docker compose up -d --build
#
#   若需要「容器内一键构建全部」的独立 CI 方案（不依赖宿主机打包），
#   请使用仓库内的 **Dockerfile.build**（多阶段，含依赖层缓存）：
#        docker build -f Dockerfile.build -t campus-trade:full .
#
# 基础镜像说明
# ---------------------------------------------------------------------
#   期望使用轻量 JRE：`eclipse-temurin:11-jre`
#   本搭建环境**无法访问 Docker Hub**（registry-1.docker.io 不可达），
#   只能使用本地已有的 `maven:3.9-eclipse-temurin-11`（同为 Temurin 11，
#   含完整 JDK，可直接 java -jar 运行），因此此处默认值为后者。
#   → 可访问 Docker Hub 时，把下一行改为：
#        FROM eclipse-temurin:11-jre
#     两者都是 Temurin 11，**替换不改变任何构建/运行行为**，仅镜像体积不同。
#
# 注意：此处**不使用 ARG 间接引用**基础镜像。
#   原因：`FROM ${VAR}` 在 docker compose 传空 build-arg 时会报
#   "base name (${VAR}) should not be blank" 而构建失败。
#   直接写死 FROM 更健壮；如需换镜像请直接改这一行。
# =====================================================================

FROM maven:3.9-eclipse-temurin-11

# 元信息
LABEL org.opencontainers.image.title="campus-trade" \
      org.opencontainers.image.description="校园/社区闲置交易平台 —— 迭代一后端工程骨架（运行镜像，不含构建步骤）" \
      org.opencontainers.image.version="1.0.0-SNAPSHOT"

# 非 root 运行（安全基线）
RUN (id -u campus >/dev/null 2>&1 || useradd -m -u 1500 campus) \
    && mkdir -p /app /app/data/uploads /app/logs \
    && chown -R 1500:1500 /app

WORKDIR /app

# 仅复制宿主机构建产物（不执行 mvn）
COPY target/campus-trade.jar /app/app.jar

RUN chown 1500:1500 /app/app.jar

USER 1500

# 应用端口
EXPOSE 8080

# 时区与 JVM 参数
#   TZ：与数据库/契约时区一致（假设 A-20 Asia/Shanghai）
#   MaxRAMPercentage：让 JVM 按容器内存上限自适应堆大小
ENV TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=dev \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8"

# 容器健康检查：/actuator/health 返回 {"status":"UP"} 即视为健康
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
    CMD (command -v curl >/dev/null 2>&1 \
         && curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"') \
        || (command -v wget >/dev/null 2>&1 \
            && wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"') \
        || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
