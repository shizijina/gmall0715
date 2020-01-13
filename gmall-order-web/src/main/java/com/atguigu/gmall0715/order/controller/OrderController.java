package com.atguigu.gmall0715.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0715.bean.*;
import com.atguigu.gmall0715.config.LoginRequire;
import com.atguigu.gmall0715.service.CartService;
import com.atguigu.gmall0715.service.ManageService;
import com.atguigu.gmall0715.service.OrderService;
import com.atguigu.gmall0715.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//@RestController
@Controller
public class OrderController {
    @Reference
    private UserService userService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;

    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request) {
        //查询用户id
        String userId = (String) request.getAttribute("userId");
        //return userService.findUserAddressByUserId(userId);
        //根据id查询用户收货地址
        List<UserAddress> userAddressesList = userService.findUserAddressByUserId(userId);
        //必须获取购物车选中的数据
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        // 第二种：页面 渲染orderDetail
        // 声明一个集合来存储订单明细
        ArrayList<OrderDetail> detailArrayList = new ArrayList<>();
        for (CartInfo cartInfo : cartInfoList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            // 添加订单明细
            detailArrayList.add(orderDetail);
        }
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(detailArrayList);
        // 计算总金额
        orderInfo.sumTotalAmount();
        // 保存数据
        request.setAttribute("userAddressesList", userAddressesList);
        request.setAttribute("detailArrayList", detailArrayList);
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());

        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo", tradeNo);
        return "trade";
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(OrderInfo orderInfo, HttpServletRequest request) {
        /*
        1.  将数据添加到数据库表中! cartInfo ,orderDetail
        2.  确定后台如何接收前台传递过来的数据！
         */
        // 调用服务层！保存
        // 订单的总金额，订单的状态，用户Id，第三方交易编号，创建时间，过期时间，进程状态也没有！

        String userId = (String) request.getAttribute("userId");
        orderInfo.setUserId(userId);

        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //防止表单重复提交
        // 获取页面提交的流水号
        String tradeNo = request.getParameter("tradeNo");
        // 调用比较方法
        boolean result = orderService.checkTradeNo(tradeNo, userId);
        // 验证失败！
        if (!result) {
            request.setAttribute("errMsg", "请勿重复提交订单！");
            return "tradeFail";
        }

        // 删除缓存的流水号
        orderService.delTradeNo(userId);

        // 验证库存！
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            boolean flag = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!flag) {
                request.setAttribute("errMsg", orderDetail.getSkuName() + "库存不足，请联系客服！");
                return "tradeFail";
            }
            // 验证价格：orderDetail.getOrderPrice()== skuInfo.price
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            //
            int res = orderDetail.getOrderPrice().compareTo(skuInfo.getPrice());
            if (res!=0){
                request.setAttribute("errMsg",orderDetail.getSkuName()+"商品价格有变动，请重新下单！");
                // 加载最新价格到缓存！
                cartService.loadCartCache(userId);
                return "tradeFail";
            }
        }
            String orderId = orderService.saveOrderInfo(orderInfo);

            // 重定向到支付模块！
            return "redirect://payment.gmall.com/index?orderId=" + orderId;
        }

    }
