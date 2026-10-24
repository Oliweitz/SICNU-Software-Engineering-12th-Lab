package com.example.module.user.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 角色权限视图（管理界面展示每个角色当前持有的权限点） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionVO {

    /** 角色码（STUDENT / TEACHER / ADMIN） */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 已授权的权限点 id 列表 */
    private List<Long> permissionIds;
}
