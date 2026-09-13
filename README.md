# 师范专业 AI 试讲台系统

> 软件工程课程设计 · 2024 级 · 选题：**师范专业 AI 试讲台系统**

面向师范类专业学生的 AI 虚拟试讲训练与考核平台：用虚拟学生 + AI 多维度评测替代/辅助真实课堂试讲，
覆盖课前磨课、技能训练、教学评价、达标认证全流程。适用场景：微格教学、教师资格证面试训练、
教学技能大赛备赛、课堂实训。

## 技术栈

| 层次 | 选型 |
|---|---|
| 后端 | Spring Boot 3.x + MyBatis-Plus 3.5.x + Spring Security(JWT) |
| 数据库 | MySQL 8.x(+ Redis 可选) |
| 实时通信 | Spring WebSocket(虚拟课堂文字事件流) |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Axios + ECharts(迭代 3 引入) |
| 构建 | Maven 3.9.x / JDK 17 目标 / JUnit 5 |

架构选型与降级策略详见 [docs/技术方案.md](docs/技术方案.md)。

## 快速开始

```bash
# 1. 初始化数据库(首次,需本机 MySQL 8.x)
mysql -u root -p < docs/init.sql

# 2. 启动后端(8080)
mvn spring-boot:run

# 3. 健康检查
curl http://localhost:8080/actuator/health
```

> 未安装 MySQL 也能先启动服务(连接池懒初始化),访问数据库接口时才报错。
> 详细环境准备见 [docs/环境配置.md](docs/环境配置.md)。

## 常用命令

```bash
mvn test                # 运行单元测试
mvn package             # 打包可执行 jar
mvn spotless:apply      # 提交前格式化代码(必做)
mvn spotless:check      # 检查格式是否合规(CI 使用)
```

## 项目结构

```
软件工程项目/
├── pom.xml                        # Maven 工程(依赖版本统一声明)
├── .gitignore / .gitattributes / .editorconfig
├── README.md                      # 本文档
├── docs/                          # 全部设计文档(交付物)
│   ├── 技术方案.md                 # 需求/架构/数据库/接口/迭代计划
│   ├── 环境配置.md                 # 环境准备与常见问题
│   ├── 代码规范.md                 # Java/前端/SQL 编码规范
│   ├── Git工作流与提交规范.md       # 分支模型/提交信息/评审流程
│   └── init.sql                   # 建库建表脚本 + 种子数据(交付物)
├── src/main/java/com/example/
│   ├── Application.java           # 启动类
│   ├── common/                    # 统一响应/异常/分页(已就绪)
│   ├── config/                    # MyBatis-Plus/CORS 配置(已就绪)
│   ├── security/                  # JWT 认证(迭代 1)
│   └── module/                    # 业务域分包(迭代 1 起逐步填充)
│       ├── user/ clazz/ task/ trial/ evaluate/
│       ├── report/ review/ resource/ stats/
└── src/main/resources/
    ├── application.yml            # 公共配置
    ├── application-dev.yml        # 开发环境(默认)
    ├── application-prod.yml       # 演示/生产环境模板
    └── mapper/                    # MyBatis XML(迭代 1 起)
```

## 开发约定(摘要)

**必须遵守**——完整内容见 `docs/` 下对应文档:

1. **提交规范**:`<type>(<scope>): <subject>`,如 `feat(user): 实现登录注册接口`,
   格式由 `.githooks/commit-msg` 自动校验(首次 clone 后执行 `git config core.hooksPath .githooks`)。
2. **代码规范**:统一响应体 `Result<T>`、业务异常 `BizException`、Controller 禁止裸返回;
   提交前执行 `mvn spotless:apply` 格式化。
3. **分支模型**:`main`(可演示的稳定版本)→ `develop`(集成)→ `feature/xxx`(按迭代任务);
   每个迭代结束打 tag(如 `v0.1.0-iter1`)。
4. **测试要求**:新增公共组件/核心业务逻辑必须带 JUnit 5 单元测试。
5. **周报**:每周按课程要求提交 `组号+项目名称+实施周报-yyyymmdd.xlsx`,内容与 Git 提交记录对应。

## 迭代计划

| 迭代 | 内容 | 状态 |
|---|---|---|
| 迭代 0:脚手架 | 工程骨架/统一响应/规范/数据库脚本 | ✅ 本仓库初始提交 |
| 迭代 1:脚手架 + 身份 | 登录注册(JWT)、班级/课程管理 | 🔲 |
| 迭代 2:核心闭环 | 任务发布、试讲提交、RuleEvaluator、评测报告 | 🔲 |
| 迭代 3:支撑功能 | 题库、批阅、看板、Excel 导出、排行榜、进步曲线 | 🔲 |
| 迭代 4:增强(可选) | 虚拟课堂(WebSocket)、音视频上传、证书、LLM 增强 | 🔲 |

详细设计见 [docs/技术方案.md](docs/技术方案.md) 第 9 节。
