package com.duke.auth.controller;

import com.duke.framework.common.Result;
import com.duke.auth.dto.UserDTO;
import com.duke.auth.service.IUserService;
import com.duke.auth.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final IUserService userService;

    @GetMapping("/{userId}")
    public Result<UserVO> getUserById(@PathVariable Long userId) {
        return Result.success(userService.getById(userId));
    }
}
