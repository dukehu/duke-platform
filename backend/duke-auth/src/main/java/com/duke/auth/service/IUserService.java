package com.duke.auth.service;

import com.duke.framework.common.PageResult;
import com.duke.auth.dto.UserDTO;
import com.duke.auth.vo.UserVO;

import java.util.List;

public interface IUserService {
    PageResult<UserVO> page(UserDTO dto);
    UserVO getById(Long id);
    void create(UserDTO dto);
    void update(UserDTO dto);
    void delete(Long id);
    void resetPassword(Long id, String newPassword);
    void assignRoles(Long userId, List<Long> roleIds);
    void assignDepts(Long userId, List<Long> deptIds, Long primaryDeptId);
    void updateStatus(Long id, Integer status);
}
