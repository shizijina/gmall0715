package com.atguigu.gmall0715.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0715.bean.UserInfo;
import com.atguigu.gmall0715.passport.config.JwtUtil;
import com.atguigu.gmall0715.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {
    @Reference
    private UserService userService;

    @Value("${token.key}")
    private String key;

    @RequestMapping("index")
    public String index(HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        //保存
        request.setAttribute("originUrl",originUrl);
        return "index";
    }
    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest request){
        //调用服务层
       UserInfo info=userService.login(userInfo);
       if(info!=null){
           HashMap<String, Object> map = new HashMap<>();
            map.put("userId",info.getId());
            map.put("nickName",info.getNickName());
            //String salt="192.168.1.125";
           String salt = request.getHeader("X-forwarded-for");
           String token= JwtUtil.encode(key,map,salt);
           return token;
       }
        return "fail";
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");
        Map<String, Object> map = JwtUtil.decode(token, key, salt);
        if(map!=null&&map.size()>0){
            //从tokenId中解密出来userId
            String userId = (String) map.get("userId");
            UserInfo userInfo=userService.verify(userId);
            if(userInfo!=null){
                return "success";
            }
        }

    return "fail";
    }
}
