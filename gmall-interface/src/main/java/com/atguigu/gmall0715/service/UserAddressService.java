package com.atguigu.gmall0715.service;

import com.atguigu.gmall0715.bean.UserAddress;

import java.util.List;

public interface UserAddressService {
    //根据用户id查询用户地址
    List<UserAddress> findUserAddressByUserId(String userId);
}
