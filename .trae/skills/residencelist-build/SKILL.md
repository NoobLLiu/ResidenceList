---
name: "residencelist-build"
description: "ResidenceList 插件编译与发布指南。当用户要求编译、打包、构建、发布、提交代码或创建 release 时触发此技能。"
---

# ResidenceList 编译与发布指南

本技能记录了 ResidenceList 插件从编译到发布的完整流程和注意事项，避免踩坑。

## 项目基本信息

- **项目路径**: `d:\AICode\JAVA-paper\ResidenceList`
- **GitHub 仓库**: `NoobLLiu/ResidenceList` (fork 自 `ArtformGames/ResidenceList`)
- **远程仓库**: `origin` = NoobLLiu, `upstream` = ArtformGames
- **构建工具**: Maven, Java 17
- **框架**: Paper 1.16+, EasyPlugin
- **模块**: 多模块 (api + plugin)

## 项目结构

```
ResidenceList/
├── api/           # API 模块 (不含依赖, 供第三方插件开发)
├── plugin/        # 插件主模块 (含 shade 打包)
├── .asset/        # shaded jar 输出目录 (maven-shade-plugin)
├── .lib/          # 本地依赖 (Residence5.1.3.0.jar)
└── .github/workflows/
    ├── maven.yml   # Build & Test (push/手动触发)
    └── deploy.yml  # 自动发布 (打 tag 触发)
```

## 关键编译知识

### 1. 本地编译 vs GitHub Actions 编译

**本地 Maven 编译可能会失败!** 项目中某些依赖托管在 GitHub Package 上 (如 EasyPlugin)，本地编译时可能遇到 SSL 证书错误：

```
unable to find valid certification path to requested target
```

**解决方案: 优先使用 GitHub Actions 编译。**

### 2. GitHub Actions 构建流程

工作流文件: `.github/workflows/maven.yml`

触发方式:
- `git push` 到任意分支自动触发
- 手动触发: `gh workflow run maven.yml --repo NoobLLiu/ResidenceList`

构建产出两个 artifact:
| artifact 名 | 路径 | 内容 | 大小 |
|---|---|---|---|
| **`assets`** | `.asset/*.jar` | **shaded jar (完整, 含依赖)** | ~1.57 MB |
| `artifacts` | `**/target/` | 未打包依赖的普通 jar | ~138 KB |

> **重要: 插件服务器只能用 `assets` 里的 shaded jar! `artifacts` 里的 jar 缺少依赖, 放入服务器不会加载。**

### 3. Shade 打包配置

`plugin/pom.xml` 中的 `maven-shade-plugin` 配置:
- 输出目录: `${project.parent.basedir}/.asset/`
- 输出文件名: `ResidenceList-<version>.jar`
- 依赖 relocate: EasyPlugin, bStats, JSON, Minedown, XSeries, SignAPI

## 完整发布流程

### Step 1: 提交代码

```bash
cd d:\AICode\JAVA-paper\ResidenceList
git add <changed-files>
git commit -m "feat(scope): description"
git push origin master
```

### Step 2: 编译构建

```bash
# 方法 A: 手动触发 GitHub Actions (推荐)
gh workflow run maven.yml --repo NoobLLiu/ResidenceList

# 方法 B: push 会自动触发, 用以下命令查看
gh run list --repo NoobLLiu/ResidenceList --limit 5
```

等待构建完成:
```bash
# 获取最新 run 的 ID
gh run list --repo NoobLLiu/ResidenceList --limit 1 --json databaseId --jq '.[0].databaseId'
```

### Step 3: 下载构建产物

```bash
# 获取最新 run ID 后
RUN_ID=<run_id>

# 下载 shaded jar (正确的插件包)
gh run download $RUN_ID --repo NoobLLiu/ResidenceList --name assets --dir .assets-download

# 下载后的文件在:
# .assets-download/ResidenceList-<old_version>.jar  (约 1.57 MB)
```

> **注意**: `--name assets` 是正确的 artifact。不要下载 `--name artifacts`, 那个是未打包依赖的。

### Step 4: 发布 Release

```bash
# 删除旧 tag (如果需要更新)
git push origin :refs/tags/v<version>

# 创建 release
gh release create v<version> --repo NoobLLiu/ResidenceList \
  --title "v<version>" \
  --notes "release notes" \
  ".assets-download\ResidenceList-<old_version>.jar#ResidenceList-<new_version>.jar"
```

> `#ResidenceList-<new_version>.jar` 是下载时显示的文件名 (label)

### Step 5: 清理

```powershell
Remove-Item -Recurse -Force .assets-download
Remove-Item -Recurse -Force .artifacts
```

## 常见错误与排查

### 1. 服务器不加载插件, 日志无任何 ResidenceList 相关输出
- **原因**: 下载了 `artifacts` 而非 `assets` 的 jar, 缺少依赖
- **解决**: 确保使用 `.asset/` 下的 shaded jar (~1.57 MB)

### 2. Maven 编译 SSL 证书错误
- **原因**: EasyPlugin 依赖托管在 GitHub, SSL 握手失败
- **解决**: 使用 GitHub Actions 编译, 或配置本地 Maven 的 SSL 证书信任

### 3. 本地 `mvn package` 成功但 jar 很小
- **原因**: shade 阶段失败但 compile 成功, 只生成了未打包的 jar
- **解决**: 检查 `.asset/` 目录是否有 shaded jar 生成

### 4. GitHub Actions 构建失败
- **原因**: 依赖下载失败 (GitHub Package 需要 token)
- **解决**: 确保仓库有 `GITHUB_TOKEN` (默认有)

## 版本号管理

版本号在 `pom.xml` 根模块中定义:
```xml
<version>1.3.8</version>
```

发布新版本时需要:
1. 修改 `pom.xml` 中的 `<version>` (会同步到 api 和 plugin 子模块)
2. 版本号在 `plugin.yml` 中通过 `${project.version}` 自动替换

## 快速命令参考

```bash
# 触发构建
gh workflow run maven.yml --repo NoobLLiu/ResidenceList

# 查看构建状态
gh run list --repo NoobLLiu/ResidenceList --limit 3

# 查看构建日志
gh run view <run_id> --repo NoobLLiu/ResidenceList --log

# 下载正确产物
gh run download <run_id> --repo NoobLLiu/ResidenceList --name assets --dir .assets-download

# 创建 release
gh release create v<version> --repo NoobLLiu/ResidenceList --title "v<version>" --notes "..." "path/to/jar.jar#Download-Name.jar"

# 查看 release
gh release view v<version> --repo NoobLLiu/ResidenceList

# 删除 release (重来)
gh release delete v<version> --repo NoobLLiu/ResidenceList --yes
```
