package com.duke.auth.service;

import com.duke.auth.dto.DeptDTO;
import com.duke.auth.vo.DeptTreeVO;

import java.util.List;

public interface IDeptService {
    List<DeptTreeVO> tree();
    void create(DeptDTO dto);
    void update(DeptDTO dto);
    void delete(Long id);
}
