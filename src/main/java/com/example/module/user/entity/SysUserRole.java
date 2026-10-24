package com.example.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户-角色关系实体（sys_user_role 表，见 docs/init.sql 5.3 第 15 表）
 *
 * <p>纯关系表：关系解除即物理删除，无 updated_at / deleted（规范 §3 豁免白名单）。
 */
@Data
@TableName("sys_user_role")
public class SysUserRole {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 id（sys_user.id） */
    private Long userId;

    /** 角色 id（sys_role.id） */
    private Long roleId;

    /** 授权时间（数据库默认值） */
    private LocalDateTime createdAt;
}
