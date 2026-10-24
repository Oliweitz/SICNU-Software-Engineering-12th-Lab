package com.example.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 角色-权限关系实体（sys_role_permission 表，见 docs/init.sql 5.3 第 16 表）
 *
 * <p>RBAC 的授权落点：角色拥有哪些权限点。纯关系表，无 updated_at / deleted（规范 §3 豁免白名单）。
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色 id（sys_role.id） */
    private Long roleId;

    /** 权限点 id（sys_permission.id） */
    private Long permissionId;

    /** 授权时间（数据库默认值） */
    private LocalDateTime createdAt;
}
