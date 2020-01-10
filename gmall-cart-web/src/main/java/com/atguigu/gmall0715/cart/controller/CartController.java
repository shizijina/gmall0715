package com.atguigu.gmall0715.cart.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0715.bean.CartInfo;
import com.atguigu.gmall0715.bean.SkuInfo;
import com.atguigu.gmall0715.config.CookieUtil;
import com.atguigu.gmall0715.config.LoginRequire;
import com.atguigu.gmall0715.service.CartService;
import com.atguigu.gmall0715.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {
    @Reference
    private CartService cartService;
    @Reference
    private ManageService manageService;
    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");
        String userId = (String) request.getAttribute("userId");
        if(userId==null){
            //用户未登录，查询cookie中是否有user-key这个数据，如果没有就说明用户未添加过商品到购物车
            userId=CookieUtil.getCookieValue(request,"user-key",false);
            // 说明未登录情况下，根本没有添加过一件商品
            if(userId==null) {
                userId = UUID.randomUUID().toString().replace("-","");
                CookieUtil.setCookie(request,response,"user-key",userId,7*24*3600,false);
            }
        }
        //添加购物车
        cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        //页面渲染使用
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request){
        //获取用户的id
        String userId = (String) request.getAttribute("userId");
        //调用服务层方法
        List<CartInfo> cartInfoList=new ArrayList<>();
        if(userId!=null){
            //用户已登录
           cartInfoList = cartService.getCartList(userId);
        }else{
            //未登录，就得去cookie中获取用户的临时id
            String userTempId = CookieUtil.getCookieValue(request, "user-key", false);
            //获取未登录购物车数据
            if(userTempId!=null){
            cartInfoList = cartService.getCartList(userTempId);
            }
        }
        request.setAttribute("cartInfoList",cartInfoList);
        return "cartList";
    }
}
