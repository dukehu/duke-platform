package com.duke.order.dto;

import com.duke.framework.dto.PageDTO;
import lombok.Data;

/**
 * Order 鍒嗛〉鏌ヨ DTO锛岀户鎵?PageDTO 鑾峰緱 current/size 瀛楁
 */
@Data
public class OrderQueryDTO extends PageDTO {

    /** 鍏抽敭璇嶆悳绱?*/
    private String keyword;
}



