package com.example.module.user.service;

import com.example.common.enums.Role;
import com.example.module.user.dto.PermissionVO;
import com.example.module.user.dto.RolePermissionVO;
import com.example.module.user.dto.UserAuthorityVO;
import com.example.module.user.entity.SysUser;
import java.util.List;

/**
 * RBAC 授权查询与维护（见 docs/技术方案.md §7 与 docs/init.sql 七、7.6）
 *
 * <p>读取路径：用户 → sys_user_role → sys_role → sys_role_permission → sys_permission；
 * 鉴权只认权限码，业务代码不得硬编码角色判断。
 */
public interface RbacService {

    /** 用户当前的角色枚举列表（无角色返回空列表） */
    List<Role> listRoles(Long userId);

    /** 用户当前持有的权限码列表（去重、按权限点 id 升序） */
    List<String> listPermissionCodes(Long userId);

    /** 组装登录/鉴权所需的身份与授权视图 */
    UserAuthorityVO loadAuthority(SysUser user);

    /** 全部权限点（管理界面授权勾选的数据源） */
    List<PermissionVO> listAllPermissions();

    /** 各角色当前持有的权限点（管理界面回显） */
    List<RolePermissionVO> listRolePermissions();

    /** 整体覆盖某角色的权限点集合（管理员操作，事务内先删后插） */
    void replaceRolePermissions(Role role, List<Long> permissionIds);
}
