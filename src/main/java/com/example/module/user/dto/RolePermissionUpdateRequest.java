package com.example.module.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** 角色权限更新请求（PUT /api/v1/rbac/roles/{roleCode}/permissions，整体覆盖） */
@Data
public class RolePermissionUpdateRequest {

    /** 该角色最终应持有的权限点 id 列表（传空数组表示收回全部权限） */
    @NotNull(message = "permissionIds 不能为空")
    private List<Long> permissionIds;
}
