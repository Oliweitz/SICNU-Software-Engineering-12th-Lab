package com.example.module.clazz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 班级-学生关系实体（clazz_student 表，多对多，见 docs/技术方案.md 第 5 节）
 *
 * <p>注：该表无逻辑删除列（关系表按需物理删除）。
 */
@Data
@TableName("clazz_student")
public class ClazzStudent {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级 id */
    private Long clazzId;

    /** 学生 id（sys_user.id） */
    private Long studentId;

    /** 入班时间（数据库默认值） */
    private LocalDateTime createdAt;
}
