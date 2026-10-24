package com.example.module.user.controller;

import com.example.common.BizException;
import com.example.common.Result;
import com.example.common.ResultCode;
import com.example.common.enums.Role;
import com.example.module.user.dto.PermissionVO;
import com.example.module.user.dto.RolePermissionUpdateRequest;
import com.example.module.user.dto.RolePermissionVO;
import com.example.module.user.dto.UserAuthorityVO;
import com.example.module.user.entity.SysUser;
import com.example.module.user.service.RbacService;
import com.example.security.LoginUser;
import com.example.security.UserContext;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RBAC 授权接口（统一前缀 /api/v1，见 docs/技术方案.md §7）
 *
 * <ul>
 *   <li>GET /api/v1/rbac/me —— 当前用户的角色与权限码（前端菜单/按钮显隐的数据源）
 *   <li>GET /api/v1/rbac/permissions —— 全部权限点（管理员）
 *   <li>GET /api/v1/rbac/roles —— 各角色当前授权（管理员）
 *   <li>PUT /api/v1/rbac/roles/{roleCode}/permissions —— 覆盖角色授权（管理员）
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/rbac")
@RequiredArgsConstructor
public class RbacController {

    private final RbacService rbacService;

    /** 当前用户授权 */
    @GetMapping("/me")
    public Result<UserAuthorityVO> me() {
        LoginUser current = UserContext.get();
        if (current == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        }
        SysUser user = new SysUser();
        user.setId(current.getId());
        user.setUsername(current.getUsername());
        user.setRealName(current.getRealName());
        return Result.ok(rbacService.loadAuthority(user));
    }

    /** 全部权限点（授权界面的勾选项） */
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority(T(com.example.common.constants.PermissionCodes).PERMISSION_MANAGE)")
    public Result<List<PermissionVO>> permissions() {
        return Result.ok(rbacService.listAllPermissions());
    }

    /** 各角色当前授权 */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority(T(com.example.common.constants.PermissionCodes).PERMISSION_MANAGE)")
    public Result<List<RolePermissionVO>> roles() {
        return Result.ok(rbacService.listRolePermissions());
    }

    /** 覆盖某角色的权限点集合 */
    @PutMapping("/roles/{roleCode}/permissions")
    @PreAuthorize("hasAuthority(T(com.example.common.constants.PermissionCodes).PERMISSION_MANAGE)")
    public Result<Void> updateRolePermissions(
            @PathVariable String roleCode,
            @Valid @RequestBody RolePermissionUpdateRequest request) {
        rbacService.replaceRolePermissions(parseRole(roleCode), request.getPermissionIds());
        return Result.ok();
    }

    private Role parseRole(String roleCode) {
        try {
            return Role.valueOf(roleCode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BizException(ResultCode.BAD_REQUEST, "角色码不合法：" + roleCode);
        }
    }
}
