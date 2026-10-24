package com.example.module.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.BizException;
import com.example.common.ResultCode;
import com.example.common.enums.Role;
import com.example.module.user.dto.PermissionVO;
import com.example.module.user.dto.RolePermissionVO;
import com.example.module.user.dto.UserAuthorityVO;
import com.example.module.user.entity.SysPermission;
import com.example.module.user.entity.SysRole;
import com.example.module.user.entity.SysRolePermission;
import com.example.module.user.entity.SysUser;
import com.example.module.user.entity.SysUserRole;
import com.example.module.user.mapper.SysPermissionMapper;
import com.example.module.user.mapper.SysRoleMapper;
import com.example.module.user.mapper.SysRolePermissionMapper;
import com.example.module.user.mapper.SysUserRoleMapper;
import com.example.module.user.mapper.UserMapper;
import com.example.module.user.service.RbacService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBAC 授权实现
 *
 * <p>读取链路三条查询：sys_user_role → sys_role_permission → sys_permission； 数据量在校内规模（用户 10^3、权限点 <
 * 100），不做缓存， 保证授权变更即时生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final UserMapper userMapper;

    @Override
    public List<Role> listRoles(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<Long> roleIds = listRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper
                .selectList(
                        Wrappers.<SysRole>lambdaQuery()
                                .in(SysRole::getId, roleIds)
                                .orderByAsc(SysRole::getId))
                .stream()
                .map(SysRole::getRoleCode)
                .map(this::parseRole)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    public List<String> listPermissionCodes(Long userId) {
        List<Long> roleIds = listRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds =
                rolePermissionMapper
                        .selectList(
                                Wrappers.<SysRolePermission>lambdaQuery()
                                        .in(SysRolePermission::getRoleId, roleIds))
                        .stream()
                        .map(SysRolePermission::getPermissionId)
                        .distinct()
                        .toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        return permissionMapper
                .selectList(
                        Wrappers.<SysPermission>lambdaQuery()
                                .in(SysPermission::getId, permissionIds)
                                .orderByAsc(SysPermission::getSortNo))
                .stream()
                .map(SysPermission::getPermCode)
                .distinct()
                .toList();
    }

    @Override
    public UserAuthorityVO loadAuthority(SysUser user) {
        // 令牌有效期 24h：每次请求回查用户，逻辑删除/停用的账号立即失效
        SysUser current = user.getId() == null ? null : userMapper.selectById(user.getId());
        if (current == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户不存在或已停用");
        }
        List<Role> roles = listRoles(current.getId());
        return UserAuthorityVO.builder()
                .userId(current.getId())
                .username(current.getUsername())
                .realName(current.getRealName())
                .roles(roles.stream().map(Enum::name).toList())
                .permissions(listPermissionCodes(current.getId()))
                .build();
    }

    @Override
    public List<PermissionVO> listAllPermissions() {
        return permissionMapper
                .selectList(
                        Wrappers.<SysPermission>lambdaQuery().orderByAsc(SysPermission::getSortNo))
                .stream()
                .map(
                        p ->
                                PermissionVO.builder()
                                        .id(p.getId())
                                        .permCode(p.getPermCode())
                                        .permName(p.getPermName())
                                        .module(p.getModule())
                                        .permType(p.getPermType())
                                        .sortNo(p.getSortNo())
                                        .build())
                .toList();
    }

    @Override
    public List<RolePermissionVO> listRolePermissions() {
        List<SysRole> roles =
                roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId));
        List<SysRolePermission> all = rolePermissionMapper.selectList(null);
        Map<Long, List<Long>> byRole =
                all.stream()
                        .collect(
                                Collectors.groupingBy(
                                        SysRolePermission::getRoleId,
                                        Collectors.mapping(
                                                SysRolePermission::getPermissionId,
                                                Collectors.toList())));
        return roles.stream()
                .map(
                        r ->
                                new RolePermissionVO(
                                        r.getRoleCode(),
                                        r.getRoleName(),
                                        byRole.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public void replaceRolePermissions(Role role, List<Long> permissionIds) {
        SysRole target =
                roleMapper.selectOne(
                        Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, role.name()));
        if (target == null) {
            throw new BizException(ResultCode.NOT_FOUND, "角色不存在：" + role.name());
        }

        List<Long> distinctIds =
                permissionIds == null
                        ? List.of()
                        : new ArrayList<>(new LinkedHashSet<>(permissionIds));
        if (!distinctIds.isEmpty()) {
            Long exists =
                    permissionMapper.selectCount(
                            Wrappers.<SysPermission>lambdaQuery()
                                    .in(SysPermission::getId, distinctIds));
            if (exists == null || exists != distinctIds.size()) {
                throw new BizException(ResultCode.BAD_REQUEST, "存在不存在的权限点 id");
            }
        }

        rolePermissionMapper.delete(
                Wrappers.<SysRolePermission>lambdaQuery()
                        .eq(SysRolePermission::getRoleId, target.getId()));
        for (Long permissionId : distinctIds) {
            SysRolePermission relation = new SysRolePermission();
            relation.setRoleId(target.getId());
            relation.setPermissionId(permissionId);
            rolePermissionMapper.insert(relation);
        }
        log.info("角色权限已更新: role={}, permissions={}", role, Set.copyOf(distinctIds));
    }

    private List<Long> listRoleIds(Long userId) {
        return userRoleMapper
                .selectList(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();
    }

    private Role parseRole(String roleCode) {
        try {
            return Role.valueOf(roleCode);
        } catch (IllegalArgumentException e) {
            log.warn("sys_role 存在未在 Role 枚举登记的取值: {}", roleCode);
            return null;
        }
    }
}
