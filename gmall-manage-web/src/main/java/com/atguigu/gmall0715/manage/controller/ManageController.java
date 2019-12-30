package com.atguigu.gmall0715.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0715.bean.*;
import com.atguigu.gmall0715.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//@Controller
@RestController
@CrossOrigin
public class ManageController {
    @Reference
    private ManageService manageService;

    @RequestMapping("getCatalog1")
    public List<BaseCatalog1> getCatalog1(){

        return manageService.getCatalog1();
    }

    @RequestMapping("getCatalog2")
    public List<BaseCatalog2> getCatalog2(String catalogId,BaseCatalog2 baseCatalog2){

        return manageService.getCatalog2(baseCatalog2);
    }
    @RequestMapping("getCatalog3")
    public List<BaseCatalog3> getCatalog3(String catalog2Id, BaseCatalog3 baseCatalog3){

        return manageService.getCatalog3(baseCatalog3);
    }
    @RequestMapping("attrInfoList")
    public List<BaseAttrInfo> attrInfoList(String catalog3Id,BaseAttrInfo baseAttrInfo){

        //return manageService.getAttrInfoList(baseAttrInfo);
        return manageService.getAttrInfoList(catalog3Id);
    }

    //必须接收前端数据，然后保存
    @RequestMapping("saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        //调用服务层
        manageService.saveAttrInfo(baseAttrInfo);
    }
    //修改时回显数据
    @RequestMapping("getAttrValueList")
    public List<BaseAttrValue>getAttrValueList(String attrId){
        //根据功能开发来讲（不使用）
       // return manageService.getAttrValueList(attrId);

       BaseAttrInfo baseAttrInfo= manageService.getBaseAttrInfo(attrId);
       if(baseAttrInfo==null){
           return null;
       }
       return baseAttrInfo.getAttrValueList();
    }
    

}
