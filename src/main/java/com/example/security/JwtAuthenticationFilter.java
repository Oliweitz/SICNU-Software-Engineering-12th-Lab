package com.example.security;

import com.example.common.BizException;
import com.example.module.user.dto.UserAuthorityVO;
import com.example.module.user.entity.SysUser;
import com.example.module.user.service.RbacService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 认证过滤器（见 docs/技术方案.md §7：Authorization: Bearer &lt;token&gt;）
 *
 * <p>流程：解析令牌 → 按 userId 现查角色与权限码 → 注入 authorities（{@code ROLE_xxx} + 权限码） → 填充 SecurityContext 与
 * {@link UserContext}，最后在 finally 中清理 ThreadLocal。
 *
 * <p>令牌无效时不直接写响应，只清空上下文继续过滤链，由 SecurityConfig 的 entryPoint 统一返回 401。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";

    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    private final RbacService rbacService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader(HEADER);
            if (header != null && header.startsWith(PREFIX)) {
                authenticate(request, header.substring(PREFIX.length()).trim());
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            LoginUser tokenUser = jwtUtil.parseToken(token);
            SysUser query = new SysUser();
            query.setId(tokenUser.getId());
            query.setUsername(tokenUser.getUsername());
            query.setRealName(tokenUser.getRealName());

            UserAuthorityVO authority = rbacService.loadAuthority(query);
            LoginUser loginUser =
                    LoginUser.builder()
                            .id(authority.getUserId())
                            .username(authority.getUsername())
                            .realName(authority.getRealName())
                            .roles(authority.getRoles())
                            .permissions(authority.getPermissions())
                            .build();

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authority
                    .getRoles()
                    .forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
            authority
                    .getPermissions()
                    .forEach(perm -> authorities.add(new SimpleGrantedAuthority(perm)));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserContext.set(loginUser);
        } catch (BizException e) {
            log.debug("令牌校验失败: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            UserContext.clear();
        }
    }
}
