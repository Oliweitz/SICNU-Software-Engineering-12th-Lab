package com.example.common.constants;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 权限码常量（与 sys_permission.perm_code 逐条对应，见 docs/init.sql 5.4 种子）
 *
 * <p>用途：{@code @PreAuthorize("hasAuthority('" + PermissionCodes.TASK_MANAGE + "')")} 写法可读、可重构； 前端通过
 * {@code GET /api/v1/rbac/me} 取同一批字符串控制菜单与按钮。
 *
 * <p>新增权限点必须先改 docs/init.sql（权威源）再补常量，二者不一致由单元测试拦截。
 */
public final class PermissionCodes {

    /** 查看用户 */
    public static final String USER_READ = "user:read";

    /** 管理用户 */
    public static final String USER_MANAGE = "user:manage";

    /** 查看班级 */
    public static final String CLAZZ_READ = "clazz:read";

    /** 管理班级 */
    public static final String CLAZZ_MANAGE = "clazz:manage";

    /** 查看任务 */
    public static final String TASK_READ = "task:read";

    /** 发布/关闭任务 */
    public static final String TASK_MANAGE = "task:manage";

    /** 提交试讲 */
    public static final String TRIAL_SUBMIT = "trial:submit";

    /** 查看试讲 */
    public static final String TRIAL_READ = "trial:read";

    /** 查看评测报告 */
    public static final String REPORT_READ = "report:read";

    /** 批阅与点评 */
    public static final String REVIEW_MANAGE = "review:manage";

    /** 查看题库与资源 */
    public static final String RESOURCE_READ = "resource:read";

    /** 维护题库与资源 */
    public static final String RESOURCE_MANAGE = "resource:manage";

    /** 查看数据字典 */
    public static final String DICT_READ = "dict:read";

    /** 维护数据字典 */
    public static final String DICT_MANAGE = "dict:manage";

    /** 查看个人统计 */
    public static final String STATS_SELF = "stats:self";

    /** 查看班级统计 */
    public static final String STATS_CLASS = "stats:class";

    /** 查看全局统计 */
    public static final String STATS_GLOBAL = "stats:global";

    /** 查看达标证书 */
    public static final String CERT_READ = "cert:read";

    /** 维护角色与权限 */
    public static final String PERMISSION_MANAGE = "permission:manage";

    /** 全部权限码（与 init.sql 种子行数一致） */
    public static final Set<String> ALL =
            Set.copyOf(
                    new LinkedHashSet<>(
                            java.util.List.of(
                                    USER_READ,
                                    USER_MANAGE,
                                    CLAZZ_READ,
                                    CLAZZ_MANAGE,
                                    TASK_READ,
                                    TASK_MANAGE,
                                    TRIAL_SUBMIT,
                                    TRIAL_READ,
                                    REPORT_READ,
                                    REVIEW_MANAGE,
                                    RESOURCE_READ,
                                    RESOURCE_MANAGE,
                                    DICT_READ,
                                    DICT_MANAGE,
                                    STATS_SELF,
                                    STATS_CLASS,
                                    STATS_GLOBAL,
                                    CERT_READ,
                                    PERMISSION_MANAGE)));

    private PermissionCodes() {}
}
