package com.atguigu.gmall0715.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0715.bean.*;
import com.atguigu.gmall0715.service.ListService;
import com.atguigu.gmall0715.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin
public class SkuManageController {
    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;
    //回显spu图片
    @RequestMapping("spuImageList")
    public List<SpuImage> getSpuImageList(String spuId ,SpuImage spuImage){

        return manageService.getSpuImageList(spuImage);
    }
    //回显销售属性
    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> getspuSaleAttrList(String spuId){

        return manageService.getspuSaleAttrList(spuId);
    }
    //添加sku
    @RequestMapping("saveSkuInfo")
    public void saveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        //保存完之后商品上架
        //发送消息队列异步处理，通知管理员做审核，审核成功之后商品上架
    }
    //根据skuId来上传
    @RequestMapping("onSale")
    public void onSale(String skuId){
        // 商品上架{saveSkuLsInfo}
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        // 给skuLsInfo 初始化赋值
        // 根据skuId 查询skuInfo
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        // 属性拷贝
        BeanUtils.copyProperties(skuInfo,skuLsInfo);
        listService.saveSkuLsInfo(skuLsInfo);
    }
}
