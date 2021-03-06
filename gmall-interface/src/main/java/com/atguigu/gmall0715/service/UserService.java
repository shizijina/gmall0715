package com.atguigu.gmall0715.service;

import com.atguigu.gmall0715.bean.UserAddress;
import com.atguigu.gmall0715.bean.UserInfo;

import java.util.List;
//业务层接口
public interface UserService {
    List<UserInfo> finaAll();

    //根据用户id查询用户地址
    List<UserAddress> findUserAddressByUserId(String userId);

    /**
     * 登录
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 解密token
     * @param userId
     * @return
     */
    UserInfo verify(String userId);

}
