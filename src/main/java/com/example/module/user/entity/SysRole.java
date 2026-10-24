package com.example.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 角色实体（sys_role 表，RBAC 授权单位，见 docs/init.sql 5.3 第 13 表） */
@Data
@TableName("sys_role")
public class SysRole {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色码：STUDENT / TEACHER / ADMIN（与 {@code Role} 枚举一一对应） */
    private String roleCode;

    /** 角色名称（中文展示名） */
    private String roleName;

    /** 职责说明 */
    private String description;

    /** 创建时间（数据库默认值） */
    private LocalDateTime createdAt;

    /** 更新时间（数据库自动维护） */
    private LocalDateTime updatedAt;

    /** 逻辑删除：0 正常 / 1 已删除 */
    @TableLogic private Integer deleted;
}
