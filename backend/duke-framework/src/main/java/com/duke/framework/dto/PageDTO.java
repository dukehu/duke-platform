package com.duke.framework.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class PageDTO {
    private Integer current = 1;
    private Integer size = 10;
    private Map<String, Object> params = new HashMap<>();

    public Integer getPageNum() { return current; }
    public Integer getPageSize() { return size; }
}
