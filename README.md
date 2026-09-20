# 师范专业 AI 试讲台系统

> 软件工程课程设计 · 2024 级 · 选题：**师范专业 AI 试讲台系统**
> 当前里程碑 `v0.1.0-iter0` · 单元测试 28/28 通过 · 分支 `develop`

面向师范类专业学生的 AI 虚拟试讲训练与考核平台：用虚拟学生 + AI 多维度评测替代/辅助真实课堂试讲，
覆盖课前磨课、技能训练、教学评价、达标认证全流程。适用场景：微格教学、教师资格证面试训练、
教学技能大赛备赛、课堂实训。

## 技术栈

| 层次 | 选型 |
|---|---|
| 后端 | Spring Boot 3.5.4 + MyBatis-Plus 3.5.12 + Spring Security(JWT, jjwt 0.12.6) |
| 数据库 | MySQL 8.x（本工程在 8.0.45 上验证；Redis 可选） |
| 实时通信 | Spring WebSocket（虚拟课堂文字事件流） |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Axios + ECharts（迭代 2 引入） |
| 构建 | Maven 3.9.x / JDK 17 编译目标（21、24 等更高版本同样兼容）/ JUnit 5 |
| 工程约束 | Spotless（Google Java Format AOSP，4 空格）+ EditorConfig + commit-msg 钩子 |

架构选型与延后项管理详见 [docs/技术方案.md](docs/技术方案.md) 第 3 节。

## 文档导航

本工程共 6 份文档，**各自承担不同的权威角色**，改代码前请先确认改哪一份：

| 文档 | 内容 | 权威性 |
|---|---|---|
| [技术方案.md](docs/技术方案.md) | 需求分析／架构／数据库摘要／评测引擎／接口／迭代计划 | 需求与架构的**唯一权威源** |
| [数据库设计规范.md](docs/数据库设计规范.md) | 命名／类型／索引／约束／字符集／注释／逻辑删除 / 变更流程 | 数据库设计的**唯一权威源** |
| [init.sql](docs/init.sql) | 建库建表 + 种子数据 | **表结构的唯一权威源** |
| [代码规范.md](docs/代码规范.md) | Java 分层／命名／异常日志／SQL／前端／测试规范 | 编码规约 |
| [Git工作流与提交规范.md](docs/Git工作流与提交规范.md) | 分支模型／提交信息／评审流程／常见问题 | 协作规约 |
| [环境配置.md](docs/环境配置.md) | 环境准备、组件安装、常见问题排查 | 环境说明 |

> **摘要与权威源的关系**：技术方案 §5 是表结构的**摘要**——允许不完整，**不允许与 `init.sql` 矛盾**。
> 凡摘要中出现的字段，必须真实存在于 `init.sql`。

## 快速开始

```bash
# 1. 初始化数据库（首次，需 MySQL 8.x）
mysql -u root -p < docs/init.sql

# 2. 启动后端（8080）
mvn spring-boot:run

# 3. 健康检查
curl http://localhost:8080/actuator/health
```

> **`init.sql` 无需额外字符集参数**——脚本开头已 `SET NAMES utf8mb4`，
> 这是为了修正 Windows 中文环境下 mysql 客户端默认 `character_set_client=gbk` 导致的中文乱码与 `ERROR 1366`。
> 手工执行其他含中文的 SQL 时，请显式加 `--default-character-set=utf8mb4`（查询同理，否则中文显示为乱码）。

> 未安装 MySQL 也能先启动服务（连接池懒初始化），访问数据库接口时才报错。
> 详细环境准备见 [docs/环境配置.md](docs/环境配置.md)。

## 常用命令

```bash
mvn test                # 运行单元测试（当前 28/28 通过）
mvn package             # 打包可执行 jar
mvn spotless:apply      # 提交前格式化代码（必做）
mvn spotless:check      # 检查格式是否合规（CI 使用）
```

## 项目结构

```
软件工程项目/
├── pom.xml                        # Maven 工程（依赖版本统一声明）
├── README.md                      # 本文档
├── .editorconfig / .gitattributes / .gitignore
├── .gitmessage                    # 提交信息模板
├── .githooks/commit-msg           # 提交信息格式校验钩子
├── docs/                          # 全部设计文档（交付物）
│   ├── 技术方案.md                 # 需求/架构/数据库/接口/迭代计划
│   ├── 数据库设计规范.md            # 命名/类型/索引/注释/逻辑删除规约（权威）
│   ├── 环境配置.md                 # 环境准备与常见问题
│   ├── 代码规范.md                 # Java/前端/SQL 编码规范
│   ├── Git工作流与提交规范.md       # 分支模型/提交信息/评审流程
│   └── init.sql                   # 建库建表脚本 + 种子数据（表结构唯一权威源）
├── src/main/java/com/example/
│   ├── Application.java           # 启动类
│   ├── common/                    # 统一响应 Result / 异常 / 分页 / 错误码 / 角色枚举
│   ├── config/                    # Security / MyBatis-Plus 分页 / CORS 配置
│   ├── security/                  # JWT 工具、认证过滤器、登录用户上下文（迭代 1 实现）
│   └── module/                    # 业务域分包，11 个模块
│       ├── user/                  # 用户与登录（迭代 1）
│       ├── clazz/                 # 班级、课程、成员关系（迭代 1）
│       ├── task/                  # 试讲任务发布与领取（迭代 2）
│       ├── trial/                 # 试讲提交、环节时长、录制（迭代 2/4）
│       ├── evaluate/              # ⭐ AI 评测引擎：契约 + 加权聚合 + 4 个评测器
│       ├── report/                # 评测报告查询（迭代 2，只读不计算）
│       ├── review/                # 教师批阅（迭代 3）
│       ├── resource/              # 题库、教案/板书/评分细则模板（迭代 3）
│       ├── stats/                 # 看板、导出、排行、进步曲线（迭代 3）
│       ├── cert/                  # 达标证书（迭代 4）
│       └── classroom/             # 虚拟课堂（迭代 4，当前为包骨架）
├── src/main/resources/
│   ├── application.yml            # 公共配置
│   ├── application-dev.yml        # 开发环境（默认）
│   ├── application-prod.yml       # 演示/生产环境模板
│   └── mapper/                    # MyBatis XML（迭代 1 起）
└── src/test/java/com/example/     # 单元测试，与主结构同包镜像
    ├── ApplicationSmokeTest.java
    ├── common/                    # Result / PageResult / BizException
    └── module/evaluate/           # Dimension / EvaluationEngine
```

## 当前进度

| 内容 | 状态 |
|---|---|
| 工程骨架、统一响应/异常/分页 | ✅ 完成 |
| 三份规范（代码/数据库/Git）+ 环境文档 | ✅ 完成 |
| `init.sql` 建库建表 + 种子数据 | ✅ 完成（已在 MySQL 8.0.45 实跑并二次重跑验证） |
| 业务模块骨架（11 个域） | ✅ 完成 |
| **AI 评测引擎地基** | ✅ 完成（属迭代 2 内容，已提前落地） |
| 身份认证与课程管理 | ⏳ 迭代 1，`security` 与 `user` 目前均为骨架 |

**评测引擎地基**是本工程目前完成度最高的部分，包含：

- `Dimension` 枚举——六维标识的**唯一权威定义**，消灭魔法字符串
- `Evaluator` 贡献契约——`Map<Dimension,Integer> contributions()` 声明负责的维度及**维度内贡献权重**，
  支持多个评测器共同贡献同一维度（如「教学表达」= 文本规则 60% + 普通话 ASR 40%）
- `EvaluationEngine` 加权聚合——评测器缺失时**按剩余权重自动归一化**，
  这正是「百度 API 不可用时回退保底方案」的实现机制，引擎无需为此写特判
- 28 项单元测试覆盖加权合并、权重归一化、契约校验、未评测判定

> `RuleEvaluator` / `AsrEvaluator` / `PostureEvaluator` / `LlmEvaluator` 四个实现仍是待填充的骨架，
> 分别在迭代 2 / 5 / 5 / 4 落地。

## 开发约定（摘要）

**必须遵守**——完整内容见 `docs/` 下对应文档：

1. **提交规范**：`<type>(<scope>): <subject>`，如 `feat(user): 实现登录注册接口`，
   格式由 `.githooks/commit-msg` 自动校验。
   **首次 clone 后必须执行一次**：
   ```bash
   git config core.hooksPath .githooks
   git config commit.template .gitmessage
   ```
2. **代码规范**：统一响应体 `Result<T>`、业务异常 `BizException`、Controller 禁止裸返回；
   提交前执行 `mvn spotless:apply` 格式化。
3. **数据库变更**：**先改 `docs/init.sql`**，再同步技术方案 §5 摘要，最后改本地库；
   已有数据的环境走增量脚本，禁止直接清库重来。
4. **分支模型**：`main`（可演示的稳定版本，**只经 PR 从 develop 合并**）← `develop`（集成）← `feature/xxx`；
   每个迭代结束在 `main` 上打 tag（如 `v0.1.0-iter1`）。`develop` 不接受直接提交。
5. **测试要求**：新增公共组件/核心业务逻辑必须带 JUnit 5 单元测试，提交前 `mvn test` 必须全绿。
   > JDK 23+ 环境下 `@SpringBootTest` 会因 Mockito 无法动态 attach agent 而失败，
   > 建议改用**手写桩**（参见 `EvaluationEngineTest`）；详见 [docs/环境配置.md](docs/环境配置.md) 第 6 节。
6. **周报**：每周按课程要求提交 `组号+项目名称+实施周报-yyyymmdd.xlsx`，内容与 Git 提交记录对应。

## 迭代计划

| 迭代 | 内容 | 状态 |
|---|---|---|
| 迭代 0：脚手架 | 工程骨架、统一响应/异常/分页、数据库脚本、代码规范、Git 规范 | ✅ 已完成（tag `v0.1.0-iter0`） |
| 迭代 1：身份与课程 | 登录注册（JWT）、班级/课程管理 | 进行中 |
| 迭代 2：核心闭环 | **前端工程引入（Vue 3 + Vite）**、任务发布、试讲提交、RuleEvaluator、评测报告 + 雷达图 | 未开始 |
| 迭代 3：支撑功能 | 题库资源、教师批阅、班级看板、Excel 导出、排行榜、进步曲线 | 未开始 |
| 迭代 4：增强（可选） | 虚拟课堂简版（WebSocket 事件流）、音视频上传、达标证书、LlmEvaluator 接入 | 未开始 |
| 迭代 5：AI 音视频评测与延后项（尽力） | 普通话 ASR 评测、姿态分析、直播（SRS/HLS）、微服务最小拆分 | 未开始 |

每个迭代执行：设计 → 编码 → JUnit 单测 → 演示记录（截图） → 合并 `develop` + 打 tag。

**需求覆盖情况**：题目原文共 54 项要求（功能 41 + 技术要求 13），其中 ✅ 完整实现 34 项、🔶 降级实现 20 项，
**无未声明的遗漏**——逐条核对表见 [docs/技术方案.md](docs/技术方案.md) 第 2.5 节。

详细设计见 [docs/技术方案.md](docs/技术方案.md) 第 9 节。
