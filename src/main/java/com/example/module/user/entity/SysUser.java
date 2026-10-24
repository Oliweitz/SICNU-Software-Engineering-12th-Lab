package com.example.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 用户实体（sys_user 表，见 docs/技术方案.md 第 5 节 / docs/init.sql）
 *
 * <p>角色不在本表：V3 起改为 RBAC 多对多（sys_user_role），用 {@code RbacService} 查询。
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名（学号/工号），唯一 */
    private String username;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 专业（师范生） */
    private String major;

    /** 年级 */
    private String grade;

    /** 创建时间（数据库默认值，插入时无需赋值） */
    private LocalDateTime createdAt;

    /** 更新时间（数据库自动维护） */
    private LocalDateTime updatedAt;

    /** 逻辑删除：0 正常 / 1 已删除 */
    @TableLogic private Integer deleted;
}
