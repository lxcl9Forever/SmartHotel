package com.smarthotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("room_type_image")
public class RoomTypeImage {

    @TableId(value = "image_id", type = IdType.AUTO)
    private Long imageId;

    private Long roomTypeId;

    private String imageUrl;

    private Integer isCover;

    private Integer sortNo;

    private LocalDateTime createTime;
}