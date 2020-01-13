package com.atguigu.gmall0715.service;

import com.atguigu.gmall0715.bean.CartInfo;

import java.util.List;

public interface CartService {
    /**
     * 添加购物车
     */
    void  addToCart(String skuId,String userId,Integer skuNum);
    /**
     *  根据用户id查询购物车
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartInfoNoLoginList
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId);

    /**
     * 删除临时购物车数据DB和缓存
     * @param userTempId
     */
    void deleteCartList(String userTempId);
    /**
     *  修改购物车状态，根据未登录购物车状态为准
     */
    void checkCart(String skuId, String userId, String isChecked);

    /**
     * 根据用户id查询购物车选中的数据
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);
    // 查询最新价格
    List<CartInfo> loadCartCache(String userId);
}
