package com.smarthotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthotel.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}