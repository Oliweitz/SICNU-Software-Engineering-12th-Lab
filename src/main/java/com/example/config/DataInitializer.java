package com.example.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.common.enums.Role;
import com.example.common.util.MaskUtil;
import com.example.module.user.entity.SysUser;
import com.example.module.user.mapper.UserMapper;
import com.example.module.user.service.RbacService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 开发环境演示账号初始化
 *
 * <p>仅当 {@code auth.seed.enabled=true}（dev 默认开启）时执行，幂等：
 *
 * <ul>
 *   <li>admin / admin123 —— 管理员
 *   <li>teacher / teacher123 —— 教师
 *   <li>student / student123 —— 师范生
 * </ul>
 *
 * <p>数据库未就绪时只记录警告，不阻断应用启动（与 Hikari 懒初始化策略一致）。 生产环境必须保持 {@code
 * auth.seed.enabled=false}，并在首次上线后立即修改演示密码。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "auth.seed.enabled", havingValue = "true")
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final RbacService rbacService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            seed(
                    "admin",
                    "admin123",
                    "系统管理员",
                    "13900000000",
                    "admin@example.com",
                    Role.ADMIN,
                    null,
                    null);
            seed(
                    "teacher",
                    "teacher123",
                    "李老师",
                    "13900000001",
                    "teacher@example.com",
                    Role.TEACHER,
                    null,
                    null);
            seed(
                    "student",
                    "student123",
                    "王同学",
                    "13900000002",
                    "student@example.com",
                    Role.STUDENT,
                    "CHINESE_EDU",
                    "2024");
            log.info("演示账号已就绪（仅开发环境）：admin/admin123、teacher/teacher123、student/student123");
        } catch (Exception e) {
            log.warn("演示账号初始化跳过（数据库可能尚未就绪或未执行 V3/V4 迁移）: {}", e.getMessage());
        }
    }

    private void seed(
            String username,
            String rawPassword,
            String realName,
            String phone,
            String email,
            Role role,
            String major,
            String grade) {
        SysUser user =
                userMapper.selectOne(
                        Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (user == null) {
            boolean contactTaken =
                    userMapper.selectCount(
                                    Wrappers.<SysUser>lambdaQuery()
                                            .eq(SysUser::getPhone, phone)
                                            .or()
                                            .eq(SysUser::getEmail, email))
                            > 0;
            if (contactTaken) {
                log.warn("演示账号 {} 的手机号/邮箱已被占用，跳过创建", username);
                return;
            }
            user = new SysUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRealName(realName);
            user.setPhone(phone);
            user.setEmail(email);
            user.setMajor(major);
            user.setGrade(grade);
            userMapper.insert(user);
            log.info(
                    "已创建演示账号: username={}, phone={}, email={}",
                    username,
                    MaskUtil.maskPhone(phone),
                    MaskUtil.maskEmail(email));
        }
        rbacService.assignRole(user.getId(), role);
    }
}
