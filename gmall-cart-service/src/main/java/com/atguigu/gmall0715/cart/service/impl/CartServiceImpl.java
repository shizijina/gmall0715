package com.atguigu.gmall0715.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0715.bean.CartInfo;
import com.atguigu.gmall0715.bean.SkuInfo;
import com.atguigu.gmall0715.cart.constant.CartConst;
import com.atguigu.gmall0715.cart.mapper.CartInfoMapper;
import com.atguigu.gmall0715.config.RedisUtil;
import com.atguigu.gmall0715.service.CartService;
import com.atguigu.gmall0715.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartInfoMapper cartInfoMapper;
    @Reference
    private ManageService manageService;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        /**
         * 1.判断购物车的商品是否存在
         * true：在原来的数量上相加 mysql
         * false：直接添加数据  mysql
         * 2.添加完之后必须更新redis
         */
        //获取redis
        Jedis jedis = redisUtil.getJedis();
        //每个用户的key
        String cartKey= CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //判断缓存中是否有这个可以值，如果没有就去数据库查询
        if(!jedis.exists(cartKey)){
            loadCartCache(userId);
        }
//        CartInfo cartInfo = new CartInfo();
//        cartInfo.setUserId(userId);
//        cartInfo.setSkuId(skuId);
//        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userId).andEqualTo("skuId",skuId);
        CartInfo cartInfoExist = cartInfoMapper.selectOneByExample(example);

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        if(cartInfoExist!=null){
            //说明购物车有该商品。使用数量应该相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //初始化skuPrice
            //cartInfoExist.setSkuPrice(cartInfoExist.getSkuPrice());
            cartInfoExist.setSkuPrice(skuInfo.getPrice());
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);

        }else {
            //说明购物车没有该商品，直接添加到数据库和缓存中

            CartInfo cartInfo1 = new CartInfo();

            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuNum(skuNum);
            cartInfo1.setSkuId(skuId);
            cartInfo1.setUserId(userId);
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfoMapper.insertSelective(cartInfo1);
            cartInfoExist=cartInfo1;
        }

        jedis.hset(cartKey,skuId, JSON.toJSONString(cartInfoExist));
        //设置购物车的过期时间
        setCartKeyExpire(userId, jedis, cartKey);

        jedis.close();

    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        /**
         * 1.先获取缓存中的数据
         * true：直接返回数据
         * false：查询数据库，并将数据库的数据存入缓存，返回集合
         */
        List<CartInfo> cartInfoList = new ArrayList<>();
        //获取redis
        Jedis jedis = redisUtil.getJedis();
        //每个用户的key
        String cartKey= CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        // hash 获取数据
        // jedis.hget() 只能获取一条数据
        // jedis.hgetAll() 能够获取field ，value
        List<String> stringList = jedis.hvals(cartKey);//只获取key中的value
        if(stringList!=null&&stringList.size()>0){
            for (String cartInfoJson : stringList) {
                //将json转换成对象并添加到集合中
                cartInfoList.add(JSON.parseObject(cartInfoJson,CartInfo.class));
            }
            //自定义比较器
            Collections.sort(cartInfoList,(CartInfo o1, CartInfo o2) -> o1.getId().compareTo(o2.getId()));
        }else{
            //没有缓存走db，并且将查询到的数据放入缓存
            cartInfoList = loadCartCache(userId);
        }
        return cartInfoList;
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId) {
        //合并的集合
     List<CartInfo> cartInfoList = new ArrayList<>();
        //登录的购物车
        List<CartInfo> cartInfoLoginList=cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoLoginList!=null && cartInfoLoginList.size()>0){
            //登录有数据
            for (CartInfo cartInfoNoLogin : cartInfoNoLoginList) {
                // 声明一个标识来判断是否有相同的商品
                boolean isMatch = false;
                for (CartInfo cartInfoLogin : cartInfoLoginList) {
                    if(cartInfoNoLogin.getSkuId().equals(cartInfoLogin.getSkuId())){
                        //数量相加
                        cartInfoLogin.setSkuNum(cartInfoNoLogin.getSkuNum()+cartInfoLogin.getSkuNum());
                        // 更新到数据库mysql
                        cartInfoMapper.updateByPrimaryKeySelective(cartInfoLogin);
                        isMatch=true;
                    }
                }
                //没有相同的商品
                if(!isMatch){
                    // 直接将未登录的数据添加到登录数据
                    cartInfoNoLogin.setId(null);
                    // 设置登录的userId 给未登录的对象
                    cartInfoNoLogin.setUserId(userId);
                    cartInfoMapper.insertSelective(cartInfoNoLogin);
                }
            }

        }else{
            //登录没数据,未登录数据直接改成登录数据
            for (CartInfo cartInfo : cartInfoNoLoginList) {
                //为了避免主键冲突，所以设置成null
                cartInfo.setId(null);
                cartInfo.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfo);
            }

        }
        // 做合并之后的汇总
        cartInfoList=loadCartCache(userId);
        // 判断状态合并购物车
        if (cartInfoList!=null && cartInfoList.size()>0){
            for (CartInfo cartInfoLogin : cartInfoList) {
                for (CartInfo cartInfo : cartInfoNoLoginList) {
                    // 保存商品相同
                    if (cartInfo.getSkuId().equals(cartInfoLogin.getSkuId())){
                        // 判断选择状态 根据未登录
                        if ("1".equals(cartInfo.getIsChecked())) {
                            if (!"1".equals(cartInfoLogin.getIsChecked())) {
                                // 更改数据库的状态
                                cartInfoLogin.setIsChecked("1");
                                // 调用选中的方法
                                checkCart(cartInfo.getSkuId(), userId, "1");
                            }
                        }
                    }
                }
            }
        }
        return cartInfoList;
    }

    @Override
    public void deleteCartList(String userTempId) {
        // 删除未登录购物车数据： redis + mysql
        // 删除数据库
        // delete from cartInfo where userId = ?
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userTempId);
        cartInfoMapper.deleteByExample(example);

        // 删除缓存
        Jedis jedis = redisUtil.getJedis();
        // key = user:userId:cart  field=skuId  value=cartInfo.toString();
        String cartKey = CartConst.USER_KEY_PREFIX+userTempId+CartConst.USER_CART_KEY_SUFFIX;
        jedis.del(cartKey);

        jedis.close();
    }

    @Override
    public void checkCart(String skuId, String userId, String isChecked) {
        // update cartInfo set isChecked = ? where skuId = ? and userId = ?
        // 第一个参数表示修改的内容 第二个参数 example 永远都代表查询，更新，删除等的条件
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        Example example = new Example(CartInfo.class);
        example.createCriteria().andEqualTo("userId",userId).andEqualTo("skuId",skuId);
        cartInfoMapper.updateByExampleSelective(cartInfo,example);

        // 修改缓存 使用hash！
        Jedis jedis = redisUtil.getJedis();
        // key = user:userId:cart  field=skuId  value=cartInfo.toString();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        // 获取到当前的商品
        String cartInfoJson = jedis.hget(cartKey, skuId);

        // 转换成对象
        CartInfo cartInfoUpd = JSON.parseObject(cartInfoJson, CartInfo.class);
        cartInfoUpd.setIsChecked(isChecked);

        jedis.hset(cartKey,skuId,JSON.toJSONString(cartInfoUpd));

        // 关闭
        jedis.close();

    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();

        // 数据类型：jedis.hset(key,field,value);
        // key = user:userId:cart  field=skuId  value=cartInfo.toString();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        // 获取所有数据！
        List<String> stringList = jedis.hvals(cartKey);
        if(stringList!=null&&stringList.size()>0){
            for (String cartInfoJson : stringList) {
                // cartInfoJson 转换为cartInfo
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                // 获取到被选中的商品
                if ("1".equals(cartInfo.getIsChecked())){
                    cartInfoList.add(cartInfo);
                }
            }
        }
        // 关闭
        jedis.close();
        return cartInfoList;
    }

    //根据用户id查询数据库放入缓存
    public List<CartInfo> loadCartCache(String userId) {
        // 查询最新数据给缓存！ 商品价格  skuInfo.price
        List<CartInfo> cartInfoList=cartInfoMapper.selectCartListWithCurPrice(userId);
        if(cartInfoList==null||cartInfoList.size()==0){
            return null;
        }
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();

        // 数据类型：jedis.hset(key,field,value);
        // key = user:userId:cart  field=skuId  value=cartInfo.toString();
        String cartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;

        HashMap<String, String> map = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            // jedis.hset(cartKey,cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
            map.put(cartInfo.getSkuId(),JSON.toJSONString(cartInfo));
        }
        jedis.hmset(cartKey,map);
        jedis.close();

        return cartInfoList;
    }

    private void setCartKeyExpire(String userId, Jedis jedis, String cartKey) {
        //设置过期时间
        //获取用户的key
        String userKey= CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;

        if(jedis.exists(userKey)){
            //获取用户的过期时间
            Long ttl = jedis.ttl(userKey);
            //将用户的过期时间赋给购物车的过期时间
            jedis.expire(cartKey,ttl.intValue());
        }else {
            jedis.expire(cartKey,7*24*3600);
        }
    }
}
