package com.smarthotel.service;


import com.smarthotel.dto.stay.ExtendStayRequest;
import com.smarthotel.vo.stay.ExtendStayPreviewVO;


public interface StayExtensionService {


    /**
     * 续住预览
     */
    ExtendStayPreviewVO preview(
            ExtendStayRequest request
    );



    /**
     * 确认续住
     */
    ExtendStayPreviewVO extend(
            ExtendStayRequest request
    );

    ExtendStayPreviewVO previewForCustomer(Long userId, ExtendStayRequest request);

    ExtendStayPreviewVO extendForCustomer(Long userId, ExtendStayRequest request);

}
