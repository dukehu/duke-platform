package com.duke.notification.dto;

import com.duke.framework.dto.PageDTO;
import lombok.Data;

/**
 * Notification 鍒嗛〉鏌ヨ DTO锛岀户鎵?PageDTO 鑾峰緱 current/size 瀛楁
 */
@Data
public class NotificationQueryDTO extends PageDTO {

    /** 鍏抽敭璇嶆悳绱?*/
    private String keyword;
}



