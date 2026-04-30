package com.duke.auth.service;

/**
 * 短信发送服务接口。
 * 当前使用 {@link impl.MockSmsServiceImpl} Mock 实现（控制台打印验证码），
 * 对接真实服务商时新建实现类并加 {@code @Primary} 即可切换，无需修改业务代码。
 */
public interface SmsService {

    /**
     * 发送验证码短信。
     *
     * @param phone 目标手机号
     * @param code  6 位数字验证码
     */
    void sendCode(String phone, String code);
}
