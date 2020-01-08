package com.atguigu.gmall0715.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0715.bean.UserAddress;
import com.atguigu.gmall0715.bean.UserInfo;
import com.atguigu.gmall0715.config.RedisUtil;
import com.atguigu.gmall0715.service.UserService;
import com.atguigu.gmall0715.user.mapper.UserAddressMapper;
import com.atguigu.gmall0715.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;


import java.util.List;
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;
    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24*7;

    @Override
    public List<UserInfo> finaAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> findUserAddressByUserId(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return userAddressMapper.select(userAddress);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        //对用户密码进行加密
        String passwd = userInfo.getPasswd();
        String newPasswd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        userInfo.setPasswd(newPasswd);
        UserInfo info = userInfoMapper.selectOne(userInfo);
        if(info!=null){
            //获取jedis
            Jedis jedis = redisUtil.getJedis();
            //定义key
            String userKey=userKey_prefix+info.getId()+userinfoKey_suffix;
            //设置用户过期时间,并且把对象转成json数据
            jedis.setex(userKey,userKey_timeOut, JSON.toJSONString(info));
            jedis.close();
            return info;
        }
        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        //获取jedis
        Jedis jedis = redisUtil.getJedis();
        String userKey=userKey_prefix+userId+userinfoKey_suffix;
        //获取缓存中的数据
        String userJson = jedis.get(userKey);
        if(!StringUtils.isEmpty(userJson)){
            //把json数据转成对象
            UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
            return userInfo;
        }
        return null;
    }
}
