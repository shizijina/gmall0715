package com.atguigu.gmall0715.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0715.bean.BaseSaleAttr;
import com.atguigu.gmall0715.bean.SpuInfo;
import com.atguigu.gmall0715.service.ManageService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class SpuManageController {
    @Reference
    private ManageService manageService;

    // http://localhost:8082/spuList?catalog3Id=62
    @RequestMapping("spuList")
    public List<SpuInfo> getSpuInfoList(String catalog3Id){
        return manageService.getSpuInfoList(catalog3Id);
    }

    // http://localhost:8082/baseSaleAttrList
    @RequestMapping("baseSaleAttrList")
    public List<BaseSaleAttr> getBaseSaleAttrList(){
        return manageService.getBaseSaleAttrList();
    }

    // http://localhost:8082/saveSpuInfo
    @RequestMapping("saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
    }
}
