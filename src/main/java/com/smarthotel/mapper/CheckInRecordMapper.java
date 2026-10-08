package com.smarthotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthotel.entity.CheckInRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckInRecordMapper
        extends BaseMapper<CheckInRecord> {
}