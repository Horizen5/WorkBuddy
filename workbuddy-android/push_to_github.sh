#!/usr/bin/env bash
# =====================================================================
#  WorkBuddy Android 项目 -> GitHub 一键推送脚本
#  用途：在你本地（能访问 GitHub 的机器）运行，把整个项目推送到你的仓库
#  注意：本脚本不会碰任何密钥/Token，连接由你本机的 git 凭据处理
# =====================================================================
set -euo pipefail

# ====== 只改这一行：替换成你自己的 GitHub 仓库地址 ======
REPO_URL="https://github.com/<你的用户名>/<你的仓库名>.git"
# 例如：REPO_URL="https://github.com/johndoe/workbuddy-android.git"
# =====================================================================

BRANCH="main"
MSG="Add WorkBuddy Android WebView app (v1.0, signed release)"

cd "$(dirname "$0")"

if [ "$REPO_URL" = "https://github.com/<你的用户名>/<你的仓库名>.git" ]; then
  echo "❌ 请先编辑本脚本顶部的 REPO_URL，填入你的 GitHub 仓库地址。"
  exit 1
fi

# 初始化仓库（如果还没有）
if [ ! -d .git ]; then
  git init -q
  echo "✅ git 仓库已初始化"
fi

# 配置分支
git checkout -B "$BRANCH" >/dev/null 2>&1 || true

# 添加远程
if git remote get-url origin >/dev/null 2>&1; then
  git remote set-url origin "$REPO_URL"
else
  git remote add origin "$REPO_URL"
fi

# 暂存 & 提交（已通过 .gitignore 排除 build/ 与 keystore）
git add .
if git diff --cached --quiet; then
  echo "ℹ️  没有需要提交的改动，跳过 commit。"
else
  git commit -q -m "$MSG"
  echo "✅ 已提交"
fi

# 推送
git push -u origin "$BRANCH"
echo ""
echo "🎉 推送完成！可在 $REPO_URL 查看。"
