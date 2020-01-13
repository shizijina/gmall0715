package com.atguigu.gmall0715.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0715.bean.OrderDetail;
import com.atguigu.gmall0715.bean.OrderInfo;
import com.atguigu.gmall0715.bean.enums.OrderStatus;
import com.atguigu.gmall0715.bean.enums.ProcessStatus;


import com.atguigu.gmall0715.config.RedisUtil;
import com.atguigu.gmall0715.service.OrderService;
import com.atguigu.gmall0715.order.mapper.OrderDetailMapper;
import com.atguigu.gmall0715.order.mapper.OrderInfoMapper;
import com.atguigu.gmall0715.util.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    @Transactional
    public String saveOrderInfo(OrderInfo orderInfo) {
        // 订单的总金额，订单的状态，用户Id，第三方交易编号，创建时间，过期时间，进程状态也没有！
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID);

        // 创建时间
        orderInfo.setCreateTime(new Date());
        // 过期时间24小时！
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());

        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        // orderInfo
        orderInfoMapper.insertSelective(orderInfo);
        // orderDetail
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        // 循环遍历
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setId(null);
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        // 返回订单编号
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();
        // 确定数据类型。String
        String tradeNoKey ="user:"+userId+":tradeCode";
        //生成一个流水号
        String tradeCode= UUID.randomUUID().toString().replace("-","");
        jedis.set(tradeNoKey,tradeCode);
        jedis.close();
        return tradeCode;
    }

    @Override
    public boolean checkTradeNo(String tradeNo, String userId) {
        // 获取缓存的流水号
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();

        // 确定数据类型。String
        String tradeNoKey ="user:"+userId+":tradeCode";

        String tradeCode = jedis.get(tradeNoKey);

        jedis.close();
        return tradeNo.equals(tradeCode);
    }

    @Override
    public void delTradeNo(String userId) {
        // 获取jedis
        Jedis jedis = redisUtil.getJedis();

        // 确定数据类型。String
        String tradeNoKey ="user:"+userId+":tradeCode";

        jedis.del(tradeNoKey);

        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        // 远程调用库存接口方法 http://www.gware.com/hasStock?skuId=10221&num=2
            String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
            return "1".equals(result);
        }
    }

