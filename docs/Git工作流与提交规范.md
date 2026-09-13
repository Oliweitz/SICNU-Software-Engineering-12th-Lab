# Git 工作流与提交规范

> 软件工程课程设计 · 2026-09
> 配套文档:[技术方案.md](./技术方案.md)、[代码规范.md](./代码规范.md)

**目的**:让提交历史成为课程答辩时的"工程过程证据"——评审老师看提交记录 + 周报即可还原开发过程。

---

## 1. 一次性配置(每位成员 clone 后执行)

```bash
# 1. 启用提交信息校验钩子(格式错误将被拒绝提交)
git config core.hooksPath .githooks

# 2. 换行符策略(Windows 下避免 CRLF 污染 diff)
git config core.autocrlf input   # 或按团队约定统一为 true

# 3. 提交时使用本仓库模板(也可用 git commit -t .gitmessage)
git config commit.template .gitmessage

# 4. 确认身份(提交记录会写入周报,必须实名)
git config user.name "张三"
git config user.email "zhangsan@example.com"
```

## 2. 分支模型(小团队轻量版 Git Flow)

```
main
  └── develop                    # 集成主线,始终可运行
        ├── feature/xxx          # 每个迭代任务一条
        ├── fix/xxx              # 缺陷修复
        └── docs/xxx             # 纯文档
```

| 分支 | 用途 | 规则 |
|---|---|---|
| `main` | 可演示的稳定版本 | 只从 `develop` 合并;**每次迭代结束打 tag** |
| `develop` | 日常集成 | 功能分支合并的目标;禁止直接 push 提交 |
| `feature/<模块>-<简述>` | 单个任务 | 从最新 `develop` 切出;完成后提 PR/互查再合并 |
| `fix/<简述>` | 缺陷 | 同上 |

**Tag 规则**:`v<版本>-iter<n>`,如 `v0.1.0-iter1`(迭代 1 交付)、`v1.0.0-final`(最终交付)。
Tag 对应迭代计划(技术方案第 9 节),答辩时按 tag 展示里程碑。

**合并策略**:功能分支先 `git rebase develop`(或 `merge`)保持线性;合并到 `develop` 用 `--no-ff`
保留分支轨迹,便于追溯每个功能。

## 3. 提交信息规范(Conventional Commits 简化版)

格式:`<type>(<scope>): <subject>`

```
feat(user): 实现登录注册接口

新增 JWT 签发与刷新逻辑,登录失败 5 次锁定 10 分钟。

Ref: #12
```

| type | 含义 | 示例 |
|---|---|---|
| `feat` | 新功能 | `feat(task): 教师发布试讲任务` |
| `fix` | 缺陷修复 | `fix(evaluate): 口头禅正则误匹配标点` |
| `docs` | 仅文档 | `docs: 更新接口设计第 7 节` |
| `style` | 格式(不影响逻辑) | `style: spotless 格式化` |
| `refactor` | 重构 | `refactor(trial): 抽取环节时长计算工具` |
| `perf` | 性能优化 | `perf(stats): 班级看板聚合查询加索引` |
| `test` | 测试 | `test(evaluate): 补充规则引擎边界用例` |
| `build` | 构建/依赖 | `build: 升级 Spring Boot 至 3.5.4` |
| `ci` | CI 配置 | `ci: 增加提交钩子校验` |
| `chore` | 杂项 | `chore: 更新 .gitignore` |

**scope** 取值(业务模块):`user / clazz / task / trial / evaluate / report / review / resource / stats / common / security / config`,也可为空。

**subject 规则**:

- ≤ 80 字符(钩子强制),祈使句,中文直接陈述("实现登录接口",不要写"实现了登录接口");
- 不以句号结尾;
- 一次提交只做一件事:改 3 个模块拆 3 条提交。

**body / footer**:改动动机、影响范围、关联问题号。破坏性变更必须在 footer 标注 `BREAKING CHANGE: 说明`。

> 格式由 `.githooks/commit-msg` 自动校验;`git commit --no-verify` 仅限紧急情况,且必须在当周周报中说明原因。

## 4. 提交时机与粒度

| 时机 | 要求 |
|---|---|
| 每个可运行的小步 | 如"实体 + Mapper"、"Service + 测试"、"Controller + 联调" |
| 每日收工前 | 本地工作必须提交(推送到远端),不留"周一才有"的代码 |
| 迭代验收后 | 合并到 `develop` 并在 `main` 打 tag |

**粒度判断**:一条提交的 subject 能否一句话说清"做了什么"——说不清就拆。

**禁止**:一次提交混入格式修改与功能修改;提交未写完的代码到 `develop`;push 到 `main`。

## 5. 合并请求 / 组内互查流程

1. 功能分支完成后推送远端,发起合并请求(或线下互查),描述里写明:改了哪里、如何自测、周报对应条目。
2. 至少一名其他组员按 [代码规范.md 第 9 节评审清单](./代码规范.md) 检查。
3. 通过后由分支作者本人合并到 `develop`,随后删除功能分支。
4. 合并后立即在 `develop` 上执行 `mvn test` 确认集成无冲突破坏。

## 6. 与周报/答辩的对应关系

课程要求每周提交 `组号+项目名称+实施周报-yyyymmdd.xlsx`:

- 周报"本周完成内容"必须能对应到该周的提交记录(`git log --since` 汇总);
- 周报"下周计划"对应迭代计划的拆解(技术方案第 9 节);
- 答辩演示从 `main` 检出对应 tag,版本、提交历史、周报三者一致;
- 每个迭代结束在周报附上 `git log --oneline` 摘要。

## 7. 常见问题

| 问题 | 处理 |
|---|---|
| 提交时钩子报"格式不符合规范" | 按提示修改提交信息;`git commit --amend` 重写 |
| 忘了切新分支,直接改在 develop 上 | `git stash` → 切功能分支 → `git stash pop` → 重新提交 |
| 换行符 diff 噪音 | 确认 `.gitattributes` 已提交、`core.autocrlf` 已配置 |
| 敏感信息(密钥/密码)误提交 | 立即 `git revert`,更换密钥,并在周报中说明 |
