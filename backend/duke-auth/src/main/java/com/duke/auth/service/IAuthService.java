package com.duke.auth.service;

import com.duke.auth.dto.ChangePasswordDTO;
import com.duke.auth.dto.GithubCallbackDTO;
import com.duke.auth.dto.LoginDTO;
import com.duke.auth.dto.SmsCodeDTO;
import com.duke.auth.dto.SmsLoginDTO;
import com.duke.auth.dto.WeixinCallbackDTO;
import com.duke.auth.vo.CaptchaVO;
import com.duke.auth.vo.GithubLoginUrlVO;
import com.duke.auth.vo.LoginVO;
import com.duke.auth.vo.MenuTreeVO;
import com.duke.auth.vo.WeixinLoginUrlVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface IAuthService {
    LoginVO login(LoginDTO dto);
    List<MenuTreeVO> getMenuTree();
    CaptchaVO getCaptcha();
    void sendSmsCode(SmsCodeDTO dto);
    LoginVO smsLogin(SmsLoginDTO dto);
    WeixinLoginUrlVO getWeixinLoginUrl();
    LoginVO weixinLogin(WeixinCallbackDTO dto);
    GithubLoginUrlVO getGithubLoginUrl();
    LoginVO githubLogin(GithubCallbackDTO dto);
    void changePassword(ChangePasswordDTO dto, HttpServletRequest request);
}
