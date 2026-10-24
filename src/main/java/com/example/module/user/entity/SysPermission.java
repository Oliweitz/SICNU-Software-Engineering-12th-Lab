package com.example.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 权限点实体（sys_permission 表，见 docs/init.sql 5.3 第 14 表）
 *
 * <p>权限码是鉴权唯一判据：接口用 {@code @PreAuthorize("hasAuthority('task:manage')")}， 前端用权限码集合控制菜单/按钮显隐。
 */
@Data
@TableName("sys_permission")
public class SysPermission {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限码（模块:动作，如 task:manage） */
    private String permCode;

    /** 权限名称 */
    private String permName;

    /** 所属模块 */
    private String module;

    /** 权限类型（字典 perm_type：API / MENU） */
    private String permType;

    /** 排序号 */
    private Integer sortNo;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除：0 正常 / 1 已删除 */
    @TableLogic private Integer deleted;
}
