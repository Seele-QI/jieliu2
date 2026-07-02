# OculiX — Claude 工作指南

> 这是 OculiX 项目的 Claude Code 工作上下文文件。Claude 在该仓库中工作时必须阅读此文件。

## 项目身份

- **项目名**: OculiX (formerly SikuliX 续作)
- **当前版本**: 3.0.4 (see `pom.xml` `<version>`)
- **许可证**: MIT
- **坐标**: `io.github.oculix-org:oculixapi:3.0.4` (Maven Central)
- **定位**: 基于屏幕像素识别的视觉自动化工具。能在选择器型工具失效的桌面 GUI、虚拟桌面、Canvas/WebGL 渲染、跨应用流程中工作。
- **仓库**: <https://github.com/oculix-org/Oculix>

## 技术栈

- **语言**: Java 11+ (`.java-version` 文件固定 11)
- **构建**: Maven 多模块 (reactor)
- **脚本宿主**: Jython 2.7 (Python 语法 + JVM 互操作), JRuby, Robot Framework, PowerShell
- **核心依赖**: OpenCV (像素匹配) / Tesseract + Legerix (OCR) / TigerVNC (远程屏幕) / JSch (SSH) / ADB (Android)
- **打包**: 各平台原生库预编译并随包分发 (Windows / macOS / Linux)

## 模块结构

仓库根 `pom.xml` 的 `<modules>` 顺序很重要 —— `build-extensions` 必须**第一个**,否则 reactor 找不到自定义 banner 扩展:

| 模块 | 作用 | 备注 |
|---|---|---|
| `build-extensions` | Maven core extension,启动时打印 gecko banner | 必须在 `.mvn/extensions.xml` 引用前先 `install` |
| `API` | 核心自动化 API 库 (Java) | 用户最常依赖的产物 `oculixapi` |
| `IDE` | 图形化 IDE | 体积大,常单独构建 |
| `MCP` | **Model Context Protocol 服务器**(`oculix-mcp-server` fat jar),把 OculiX 暴露为 MCP 工具,供 Claude Desktop / Claude Code / Cursor / 自研客户端通过 stdio 或 HTTP 调用。每个工具调用写入 Ed25519 签名 + SHA-256 链式 JSONL 审计日志 | 3.0.2 新增;**LLM 集成主入口** |
| `Reporter` | 测试报告器 | release profile 下会**跳过 deploy** |
| `Additional-Wrappers` | 第三方语言包装 (非 reactor 模块) | 不参与 mvn 构建 |
| `pages/` `docs/` | oculix.org 站点源码 (Jekyll / MkDocs 类) | 与 Java 无关 |
| `tmp_workflows/` `Support/archiv/` | 历史脚本/实验 | **不要**改动 |

## 构建命令

```bash
# 第一次克隆后必须先装 build-extensions
mvn -pl build-extensions -am install

# 编译 API (CI 用)
mvn -pl API -am clean package

# 完整构建 (跳过测试,快)
mvn clean install -DskipTests

# 仅跑测试
mvn test

# IDE 模块单独构建
mvn -pl IDE -am clean package

# 发布到 Maven Central (需配置 GPG key + Sonatype token)
mvn -P release clean deploy
```

## 代码组织约定

- **Java 包命名空间保留**: `org.sikuli.*` 是历史包袱,不要重命名。这是有意保留以兼容老的 SikuliX 脚本和依赖该命名空间的下游代码。
- **类注释头**: 多数 Java 文件顶部的版权头是 `Copyright (c) 2010-2026, sikuli.org, sikulix.com, oculix-org - MIT license` —— 保持原样或同步更新年份。
- **资源文件**: 图标和图片在 `IDE/src/main/resources/icons/`,修改前确认没被多处引用 (`gecko_cyclope_hero.png` 是 README 用的 hero 图)。
- **本地化**: 用户可见字符串应集中放 (找 `Messages*.properties` 之类的资源包),不要硬编码在代码里。

## 安全: 强制版本

`pom.xml` 顶部 `<dependencyManagement>` 钉死了三组传递依赖以杀掉已知 CVE, **修改前先确认打分**:

| 依赖 | 锁定版本 | 杀掉 CVE 数 | 触发原因 |
|---|---|---|---|
| `io.netty:netty-bom` | 4.1.133.Final | 5 GHSAs | jython-slim 2.7.4 传递 |
| `org.bouncycastle:bcprov-jdk18on` / `bcpkix-jdk18on` / `bcutil-jdk18on` | 1.84 | 4 GHSAs | jython-slim 2.7.4 传递 |
| `org.codehaus.plexus:plexus-utils` | 3.6.1 | 1 GHSA | maven-core 传递 (build-extensions, provided) |

升级任一项时,确认 (1) 子模块 `pom.xml` 里没有覆盖版本 (2) 升级后 `mvn dependency:tree` 跑通 (3) CI 没新增 CVE 告警。

## MCP 模块 (LLM 集成主入口) ⭐

`MCP/` 是 3.0.2 新增的 **Model Context Protocol 服务器**,把 OculiX 包装成 LLM 可调用的工具集。**这是接入 AI 大模型的主入口**,不是单纯的审计模块。

- **协议**: 标准 [MCP](https://modelcontextprotocol.io/),**LLM 无关** —— Claude / GPT / Mistral / Gemini / Llama 都能驱动
- **传输**: stdio (Claude Desktop 默认) / HTTP (Streamable HTTP,默认 `127.0.0.1:7337/mcp`)
- **工具集** (10+ 个):
  - 视觉类:`oculix_find_image` `oculix_click_image` `oculix_dblclick_image` `oculix_rclick_image` `oculix_exists_image` `oculix_wait_for_image`
  - 输入类:`oculix_type_text` `oculix_key_combo`
  - 截图 / OCR:`oculix_screenshot` `oculix_find_text` `oculix_read_text_in_region`
  - 机密模式专用:`oculix_screenshot_to_disk` `oculix_ocr_to_disk` (像素/文本不离开本机,只回传哈希)
- **安全模型**:
  - **ActionGate V1**:默认 `AutoApproveGate` 全自动,V2 计划加 human-in-the-loop 队列
  - **审计链**: Ed25519 签名 + SHA-256 链式 JSONL,`~/.oculix-mcp/journal/audit-*.jsonl`,10000 条或 24h 自动轮转
  - **会话令牌**: HMAC 签名,30 分钟 TTL,`DELETE` 后强制失效
  - **模式**: `open` (默认,9 工具) / `confidential` (像素不出本机,合规场景)
- **构建**: `mvn -pl MCP -am -DskipTests -Pmcp-fatjar clean package` → `MCP/target/oculix-mcp-server.jar`
- **运行**: `java -jar oculix-mcp-server.jar run` (stdio) 或 `serve` (HTTP)
- **验证**: `java -jar oculix-mcp-server.jar verify` 校验审计链完整性
- **⚠️ 警告**: MCP 服务器让 LLM 拥有对全屏的点击 / 输入控制权,**绝不能在生产工作站跑**,必须用隔离 VM / 测试环境
- 修改 MCP 模块前必须读 `MCP/README.md` 第 "Audit journal format" 一节,签名/链格式不向后兼容属于**破坏性变更**。

## 发行流程

详见 `.github/workflows/release.yml` 和 `release-rc.yml`:

1. 更新 `CHANGELOG.md` 顶部 `## [vX.Y.Z]` 段 (工作流会把该段原样作为 GitHub Release 正文)
2. 更新根 `pom.xml` `<version>` 和子模块 `<parent><version>`
3. 标签格式: `v3.0.4` (stable) / `v3.0.4-rc1` (RC) / `v3.0.4-beta1` (beta)
4. 触发: push 标签 → 工作流跑 `mvn -P release clean deploy` → central-publishing-maven-plugin 推送到 Maven Central → GPG 签名
5. `release` profile 下 `oculixreporter` 和 `oculix-build-extensions` 模块会自动跳过 deploy (这是有意为之,见 CHANGELOG 3.0.4 条目)

## 仓库根关键文件

| 文件 | 用途 |
|---|---|
| `pom.xml` | reactor POM,模块顺序 + 依赖管理 + release profile |
| `.java-version` | 固定 Java 11 |
| `.mvn/extensions.xml` | 引用 build-extensions 打印 banner |
| `tigervnc-java-oculix.bundle` | TigerVNC 预编译 bundle (~230 KB),API 模块打包用 |
| `test-cli.sikuli/` | 历史 SikuliX 示例脚本,用作 IDE 启动冒烟测试 |
| `CHANGELOG.md` | Keep-a-Changelog 格式,被工作流读取作为 release notes |
| `CONTRIBUTING.md` | PR 流程 / 代码风格 / 评审期望 |
| `SECURITY.md` | 漏洞披露流程,优先用 GitHub Private Advisory |
| `CODE_OF_CONDUCT.md` | 贡献者公约 |
| `Support/` | 维护者工具脚本 (deploy / 归档 / 实验) —— 不是用户文档 |

## 常见陷阱

- ❌ 不要把 `build-extensions` 移到 `<modules>` 末尾,否则首次 `mvn install` 找不到扩展
- ❌ 不要修改 `org.sikuli.*` 包名,会破坏所有下游兼容
- ❌ 不要在 release profile 中启用 `oculixreporter` 的 deploy,它不该上 Maven Central
- ❌ 不要删除 `tigervnc-java-oculix.bundle` 或把它从 `.gitignore` 移走,API 模块构建会断
- ❌ 不要在 CHANGELOG 段写过于自由格式的内容,工作流会原样发布
- ❌ 不要在 `MCP/` 改链式哈希格式,这是破坏性变更,需走 major 版本
- ⚠️ 修改子模块版本号时记得同步改 `Support/pom_template.xml` (新模块脚手架)
- ⚠️ IDE 模块依赖很多 native 库,Windows 上构建需 Visual Studio Build Tools;用 `makeide-win.xml` 走 pre-bundled 路径可绕开

## 验证

构建完成后验证:
```bash
mvn -pl API -am test          # 跑 API 模块测试
mvn -pl MCP -am test          # 跑 MCP 审计模块测试
mvn verify                    # 全模块 verify (含签名 / 校验)
```

## 联系方式 / 进一步阅读

- 用户文档: <https://oculix.org>
- 问题跟踪: <https://github.com/oculix-org/Oculix/issues>
- 安全漏洞: GitHub Private Advisory (不用公开 issue)
- 贡献流程: `CONTRIBUTING.md`
- 上游血缘: Sikuli (Tom Yeh, MIT UIST 2009) → SikuliX (Raimund Hocke, 2010–2025) → OculiX (2026-)
