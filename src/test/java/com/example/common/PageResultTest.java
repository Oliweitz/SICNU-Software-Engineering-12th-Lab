package com.example.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link PageResult} 分页结构单元测试 */
class PageResultTest {

    @Test
    @DisplayName("从 MyBatis-Plus 分页结果转换 total 与 records")
    void convertFromPage() {
        Page<String> page = new Page<>(1, 10);
        page.setTotal(25);
        page.setRecords(List.of("a", "b"));

        PageResult<String> result = PageResult.of(page);

        assertEquals(25, result.getTotal());
        assertEquals(List.of("a", "b"), result.getRecords());
    }
}
