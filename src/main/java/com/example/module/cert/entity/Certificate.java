package com.example.module.cert.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 达标证书实体（certificate 表，见 docs/技术方案.md 第 5 节 / docs/init.sql）
 *
 * <p>对应题目需求 9「达标与认证」：技能达标证书（教学表达 / 互动 / 控场 / 时间）。 表结构在迭代 0 已定型，故实体提前落地；业务逻辑迭代 4 实现。
 *
 * <p>注：该表无 updated_at 列（证书一经颁发不再修改）。
 */
@Data
@TableName("certificate")
public class Certificate {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生 id（sys_user.id） */
    private Long studentId;

    /** 任务 id（task.id） */
    private Long taskId;

    /** 证书类型（教学表达/课堂互动/控场/时间管理） */
    private String type;

    /** 证书文件路径 */
    private String filePath;

    /** 颁发时间（数据库默认值） */
    private LocalDateTime issuedAt;

    /** 创建时间（数据库默认值） */
    private LocalDateTime createdAt;

    /** 逻辑删除：0 正常 / 1 已删除 */
    @TableLogic private Integer deleted;
}
