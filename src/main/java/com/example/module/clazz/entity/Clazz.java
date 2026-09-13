package com.example.module.clazz.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 班级实体（clazz 表，见 docs/技术方案.md 第 5 节 / docs/init.sql） */
@Data
@TableName("clazz")
public class Clazz {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 班级名称 */
    private String name;

    /** 课程（语文/数学/英语/思政/幼教等） */
    private String course;

    /** 带班教师（sys_user.id） */
    private Long teacherId;

    /** 创建时间（数据库默认值） */
    private LocalDateTime createdAt;

    /** 更新时间（数据库自动维护） */
    private LocalDateTime updatedAt;

    /** 逻辑删除：0 正常 / 1 已删除 */
    @TableLogic private Integer deleted;
}
