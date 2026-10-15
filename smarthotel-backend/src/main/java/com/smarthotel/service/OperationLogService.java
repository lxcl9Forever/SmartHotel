package com.smarthotel.service;

import com.smarthotel.entity.OperationLogRecord;
import com.smarthotel.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final OperationLogMapper mapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(OperationLogRecord record) {
        mapper.insert(record);
    }
}
