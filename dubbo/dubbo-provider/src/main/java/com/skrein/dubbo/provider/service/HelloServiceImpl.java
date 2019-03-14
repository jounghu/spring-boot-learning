package com.skrein.dubbo.provider.service;

import com.skrein.dubbo.api.service.HelloService;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Author: hujiansong
 * @Date: 2019/3/14 15:36
 * @since: 1.8
 */
@Service(version = "${demo.service.version}")
@Component
public class HelloServiceImpl implements HelloService {

    @Value("${server.port}")
    String port;

    @Value("${spring.application.name}")
    String appName;

    @Override
    public String sayHello(String name) {
        return appName + ":" + port + "hello" + name;
    }
}
