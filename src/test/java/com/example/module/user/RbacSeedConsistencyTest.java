package com.example.module.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.common.constants.PermissionCodes;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RBAC 种子与代码常量一致性测试
 *
 * <p>docs/init.sql 是权限数据的唯一权威源：本测试直接解析脚本，确保「脚本有、代码也有」， 避免权限码在 SQL 与 Java 之间漂移。
 */
class RbacSeedConsistencyTest {

    private static final Path INIT_SQL = Path.of("docs", "init.sql");

    private static String readInitSql() throws IOException {
        return Files.readString(INIT_SQL, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("权限点种子与 PermissionCodes 常量一一对应")
    void permissionSeedsMatchConstants() throws IOException {
        String sql = readInitSql();
        Matcher matcher = Pattern.compile("\\((\\d+),\\s*'([a-z]+:[a-z]+)',").matcher(sql);
        Set<String> seeded = new HashSet<>();
        while (matcher.find()) {
            seeded.add(matcher.group(2));
        }

        assertEquals(PermissionCodes.ALL, seeded, "权限码在 init.sql 与 PermissionCodes 之间不一致");
    }

    @Test
    @DisplayName("角色-权限授权数量：STUDENT 7 / TEACHER 10 / ADMIN 19")
    void rolePermissionCounts() throws IOException {
        String sql = readInitSql();
        int start = sql.indexOf("INSERT INTO sys_role_permission");
        assertTrue(start > 0, "init.sql 缺少 sys_role_permission 种子");
        String block = sql.substring(start, sql.indexOf(';', start));

        Map<String, Integer> counts = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("\\((\\d+),\\s*(\\d+)\\)").matcher(block);
        while (matcher.find()) {
            counts.merge(matcher.group(1), 1, Integer::sum);
        }

        assertEquals(7, counts.getOrDefault("1", 0), "STUDENT 权限数应为 7");
        assertEquals(10, counts.getOrDefault("2", 0), "TEACHER 权限数应为 10");
        assertEquals(19, counts.getOrDefault("3", 0), "ADMIN 应持有全部 19 个权限点");
    }

    @Test
    @DisplayName("sys_user 不再持有 role 列，字典 user_role 已移除")
    void legacyRoleColumnRemoved() throws IOException {
        String sql = readInitSql();

        int tableStart = sql.indexOf("CREATE TABLE sys_user (");
        assertTrue(tableStart > 0, "init.sql 缺少 sys_user 建表语句");
        String table = sql.substring(tableStart, sql.indexOf("ENGINE = InnoDB", tableStart));

        assertFalse(table.contains("\n    role "), "sys_user 仍保留 role 列");
        assertFalse(sql.contains("('user_role'"), "字典 user_role 仍在种子数据中");
        assertTrue(sql.contains("('perm_type'"), "缺少 perm_type 字典类型种子");
    }
}
