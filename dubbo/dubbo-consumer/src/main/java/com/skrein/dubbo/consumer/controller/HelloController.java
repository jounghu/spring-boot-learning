package com.skrein.dubbo.consumer.controller;

import com.skrein.dubbo.api.service.HelloService;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: hujiansong
 * @Date: 2019/3/14 15:39
 * @since: 1.8
 */
@RestController
public class HelloController {

    @Reference(version = "${demo.service.version}",loadbalance = "roundrobin")
    private HelloService helloService;

    @GetMapping("/hello/{name}")
    public String hello(@PathVariable(name = "name") String name) {
        return helloService.sayHello(name);
    }
}
