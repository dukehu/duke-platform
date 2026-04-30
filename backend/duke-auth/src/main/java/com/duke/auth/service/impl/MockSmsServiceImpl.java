package com.duke.auth.service.impl;

import com.duke.auth.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 短信服务 Mock 实现，仅将验证码打印到日志，供开发/测试使用。
 * 接入真实服务商（阿里云 / 腾讯云等）时，新建实现类并标注 @Primary 覆盖此 Bean。
 */
@Slf4j
@Primary
@Service
public class MockSmsServiceImpl implements SmsService {

    @Override
    public void sendCode(String phone, String code) {
        log.info("[短信验证码 Mock] phone={}, code={} — 接入真实服务商时替换此实现", phone, code);
    }
}
