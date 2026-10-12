#!/usr/bin/env bash
# =====================================================================
# 迭代一 交付打包脚本
# =====================================================================
# 文件     : scripts/package-iteration1.sh
# 版本     : v1.0
# 日期     : 2026-10-12
# 作者     : 交付负责人（待填写姓名）
# 状态     : 草稿（待确认）
#
# 用途     : 把迭代一的全部交付物打包为可提交的 zip。
#
# 用法     : bash scripts/package-iteration1.sh [输出目录]
#            默认输出到仓库根目录下的 iteration1-delivery.zip
#
# 环境变量 :
#   OUT_DIR   输出目录（默认：仓库根目录）
#   ZIP_NAME  输出文件名（默认：iteration1-delivery.zip）
#   VERBOSE   设为 1 时打印被打包的完整文件清单
#
# 退出码   : 0 = 成功；非 0 = 失败
# =====================================================================

set -euo pipefail

# ---------- 定位仓库根目录（脚本可在任意目录被调用） ----------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${REPO_ROOT}"

OUT_DIR="${1:-${OUT_DIR:-${REPO_ROOT}}}"
ZIP_NAME="${ZIP_NAME:-iteration1-delivery.zip}"
OUT_DIR="$(cd "${OUT_DIR}" 2>/dev/null && pwd || { mkdir -p "${OUT_DIR}" && cd "${OUT_DIR}" && pwd; })"
ZIP_PATH="${OUT_DIR}/${ZIP_NAME}"

echo "======================================================================"
echo " 迭代一 交付打包"
echo "======================================================================"
echo "  仓库根目录 : ${REPO_ROOT}"
echo "  输出文件   : ${ZIP_PATH}"
echo

# ---------- 交付清单 ----------
# 说明：这些是迭代一要求打包的路径（文档 + 工程骨架 + 接口骨架 + 构建与部署文件）。
INCLUDE=(
  "docs/iteration1"
  "src"
  "pom.xml"
  "docker-compose.yml"
  "Dockerfile"
  "Dockerfile.build"
  ".dockerignore"
  "README-backend.md"
  "README-db.md"
  "db"
  "scripts"
  "tools"
)

# ---------- 排除规则 ----------
# 注意：以下排除项覆盖「构建产物 / 缓存 / 版本控制 / IDE / 日志 / 运行时数据 / 临时文件」。
EXCLUDE=(
  # 构建产物与本地依赖缓存
  "target" ".mvnrepo" "node_modules" "dist" "build"
  # 版本控制与 IDE
  ".git" ".github" ".idea" ".vscode" "*.iml"
  # Python 缓存
  "__pycache__" "*.pyc"
  # 日志与运行时数据
  "*.log" "logs" "data" "uploads"
  # 操作系统文件
  ".DS_Store"
  # 本脚本自身的产物（避免递归打包）
  "${ZIP_NAME}"
)

# ---------- 前置检查 ----------
MISSING=()
for p in "${INCLUDE[@]}"; do
  [ -e "${p}" ] || MISSING+=("${p}")
done
if [ ${#MISSING[@]} -gt 0 ]; then
  echo "❌ 缺少必需路径，无法打包：" >&2
  for m in "${MISSING[@]}"; do echo "   - ${m}" >&2; done
  exit 1
fi

if ! command -v zip >/dev/null 2>&1; then
  echo "❌ 未找到 zip 命令。请先安装：brew install zip / apt-get install zip" >&2
  exit 1
fi

# ---------- 组装 rsync 排除参数（rsync 不可用时回退到 zip -x） ----------
RSYNC_ARGS=()
ZIP_X=()
for e in "${EXCLUDE[@]}"; do
  RSYNC_ARGS+=(--exclude "${e}")
  ZIP_X+=(-x "${e}" -x "${e}/*")
done

# ---------- 删除旧产物 ----------
rm -f "${ZIP_PATH}"

STAGE="$(mktemp -d)"
trap 'rm -rf "${STAGE}"' EXIT
mkdir -p "${STAGE}/iteration1-delivery"

echo "▶ 复制交付文件到暂存目录 ..."
if command -v rsync >/dev/null 2>&1; then
  # 注意：必须先建好源的「父目录」再复制，否则 rsync 会把
  # `docs/iteration1` 直接展开为暂存目录下的 `iteration1/`，丢掉 `docs/` 层级。
  for p in "${INCLUDE[@]}"; do
    DEST="${STAGE}/iteration1-delivery/$(dirname "${p}")"
    mkdir -p "${DEST}"
    rsync -a "${RSYNC_ARGS[@]}" "${p}" "${DEST}/"
  done
else
  echo "  （未找到 rsync，回退到 cp + 手动清理）"
  for p in "${INCLUDE[@]}"; do
    DEST="${STAGE}/iteration1-delivery/$(dirname "${p}")"
    mkdir -p "${DEST}"
    cp -R "${p}" "${DEST}/"
  done
  ( cd "${STAGE}/iteration1-delivery" && \
    for e in "${EXCLUDE[@]}"; do
      find . -name "${e}" -prune -exec rm -rf {} + 2>/dev/null || true
    done )
fi

# ---------- 生成交付清单 ----------
MANIFEST="${STAGE}/iteration1-delivery/DELIVERY-MANIFEST.txt"
{
  echo "迭代一 交付包清单"
  echo "生成时间: $(date '+%Y-%m-%d %H:%M:%S %Z')"
  echo "生成主机: $(hostname 2>/dev/null || echo unknown)"
  echo "======================================================================"
  echo
  echo "【文件统计】"
  ( cd "${STAGE}/iteration1-delivery" && \
    printf "  Java 源文件      : %s\n" "$(find . -name '*.java' | wc -l | tr -d ' ')" && \
    printf "  Markdown 文档    : %s\n" "$(find . -name '*.md' | wc -l | tr -d ' ')" && \
    printf "  SQL 文件         : %s\n" "$(find . -name '*.sql' | wc -l | tr -d ' ')" && \
    printf "  总文件数         : %s\n" "$(find . -type f | wc -l | tr -d ' ')" )
  echo
  echo "【交付物清单】"
  ( cd "${STAGE}/iteration1-delivery" && find . -type f | sort | sed 's|^\./|  |' )
} > "${MANIFEST}"

# ---------- 打包 ----------
echo "▶ 生成 zip ..."
( cd "${STAGE}" && zip -q -r "${ZIP_PATH}" iteration1-delivery -x '.DS_Store' )

# ---------- 结果 ----------
SIZE_BYTES=$(wc -c < "${ZIP_PATH}" | tr -d ' ')
SIZE_HUMAN=$(du -h "${ZIP_PATH}" | cut -f1 | tr -d ' ')
FILES=$(cd "${STAGE}/iteration1-delivery" && find . -type f | wc -l | tr -d ' ')
JAVA=$(cd "${STAGE}/iteration1-delivery" && find . -name '*.java' | wc -l | tr -d ' ')
MD=$(cd "${STAGE}/iteration1-delivery" && find . -name '*.md' | wc -l | tr -d ' ')
SQL=$(cd "${STAGE}/iteration1-delivery" && find . -name '*.sql' | wc -l | tr -d ' ')

echo
echo "======================================================================"
echo " ✅ 打包完成"
echo "======================================================================"
printf "  输出文件   : %s\n" "${ZIP_PATH}"
printf "  文件大小   : %s (%s 字节)\n" "${SIZE_HUMAN}" "${SIZE_BYTES}"
printf "  文件总数   : %s\n" "${FILES}"
printf "    其中 Java : %s\n" "${JAVA}"
printf "    其中 文档 : %s\n" "${MD}"
printf "    其中 SQL  : %s\n" "${SQL}"
echo
echo "  已排除     : target/ .mvnrepo/ .git/ .idea/ .vscode/ __pycache__/ *.log data/ uploads/"
echo "  内含清单   : iteration1-delivery/DELIVERY-MANIFEST.txt"
echo "  验证命令   : unzip -l ${ZIP_PATH}"
echo

if [ "${VERBOSE:-0}" = "1" ]; then
  echo "【完整文件清单】"
  ( cd "${STAGE}/iteration1-delivery" && find . -type f | sort | sed 's|^\./|  |' )
  echo
fi

exit 0
