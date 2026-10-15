package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLogRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String operation;
    private String module;
    private String requestUrl;
    private String ip;
    private Boolean success;
    private LocalDateTime createTime;
}
