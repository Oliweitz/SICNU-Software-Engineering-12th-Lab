package com.example.module.evaluate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link Dimension} 维度枚举单元测试 */
class DimensionTest {

    @Test
    @DisplayName("六维枚举齐全，key 与前后端契约一致")
    void allSixDimensionsPresent() {
        List<String> keys =
                Arrays.stream(Dimension.values())
                        .map(Dimension::getKey)
                        .collect(Collectors.toList());

        assertEquals(
                List.of("content", "expression", "posture", "interaction", "board", "time"), keys);
    }

    @Test
    @DisplayName("fromKey 能解析合法 key 并返回对应中文名")
    void fromKeyResolvesValidKey() {
        Optional<Dimension> dimension = Dimension.fromKey("interaction");

        assertTrue(dimension.isPresent());
        assertEquals("课堂互动", dimension.get().getName());
    }

    @Test
    @DisplayName("fromKey 对未知 key 返回 empty，不抛异常")
    void fromKeyReturnsEmptyForUnknownKey() {
        assertTrue(Dimension.fromKey("not_a_dimension").isEmpty());
        assertTrue(Dimension.fromKey(null).isEmpty());
    }
}
