package com.example.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.module.user.entity.SysUser;

/**
 * 用户 Mapper（Mapper 扫描配置见 Application 的 @MapperScan）
 *
 * <p>单表 CRUD 直接使用 MyBatis-Plus BaseMapper；复杂查询在 resources/mapper 下写 XML（见 docs/代码规范.md 第 5 节）。
 */
public interface UserMapper extends BaseMapper<SysUser> {}
