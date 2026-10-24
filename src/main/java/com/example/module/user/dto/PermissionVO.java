package com.example.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 权限点视图（管理界面按模块展示权限清单） */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionVO {

    /** 权限点 id */
    private Long id;

    /** 权限码（模块:动作） */
    private String permCode;

    /** 权限名称 */
    private String permName;

    /** 所属模块 */
    private String module;

    /** 权限类型（API / MENU） */
    private String permType;

    /** 排序号 */
    private Integer sortNo;
}
