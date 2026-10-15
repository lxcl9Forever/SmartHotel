package com.smarthotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smarthotel.entity.OperationLogRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogRecord> {
}
