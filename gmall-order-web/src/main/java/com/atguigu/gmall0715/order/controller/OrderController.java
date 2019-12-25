package com.atguigu.gmall0715.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0715.bean.UserAddress;
import com.atguigu.gmall0715.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
public class OrderController {
    @Reference
    private UserService userService;

    //根据用户id查询地址
    @RequestMapping("trade")
    public List<UserAddress> trade(@RequestParam("userId") String userId){

        return userService.findUserAddressByUserId(userId);
    }
}
