package com.atguigu.gmall0715.service;

import com.atguigu.gmall0715.bean.OrderInfo;

public interface OrderService {
    // 订单接口
    String saveOrderInfo(OrderInfo orderInfo);

    //生成一个流水号
    String getTradeNo(String userId);
    // 比较流水号
    boolean checkTradeNo(String tradeNo,String userId);
    // 删除流水号
    void delTradeNo(String userId);
    // 验证库存
    boolean checkStock(String skuId, Integer skuNum);
}
