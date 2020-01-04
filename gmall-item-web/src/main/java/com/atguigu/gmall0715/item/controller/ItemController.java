package com.atguigu.gmall0715.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0715.bean.SkuInfo;
import com.atguigu.gmall0715.bean.SkuSaleAttrValue;
import com.atguigu.gmall0715.bean.SpuSaleAttr;
import com.atguigu.gmall0715.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {
    @Reference
    private ManageService manageService;
    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, HttpServletRequest request){
        // 将商品的图片列表封装到skuInfo 的 skuImageList
       // System.out.println(skuId);
       SkuInfo skuInfo=manageService.getSkuInfo(skuId);

        // 查询销售属性-销售属性值并锁定
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        // 查询 销售属性值与skuId 组合的数据集合
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        // 拼接字符串
        String key = "";
        HashMap<String, String> map = new HashMap<>();
        if (skuSaleAttrValueListBySpu!=null && skuSaleAttrValueListBySpu.size()>0){
            // {"125|128":"40","124|129":"39","123|127":"38","123|126":"37"}
            //  key =125|128  value =40
            //  map.put(key,value);  map---->json
            // 对应的拼接规则：1.   如果skuId 与下一个skuId 不一致的时候，则停止拼接。 2.  当循环到集合末尾的时候，停止拼接 map.put(key,value) 清空key
            // 第一次循环 key = 123  第二次 key = 123|126  第三次循环 map.put(key,value) 清空key

            for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {
                SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);

                // 什么时候拼接|
                if (key.length()>0){
                    key+="|";
                }
                // 拼接key
                key+=skuSaleAttrValue.getSaleAttrValueId();

                // 什么时候停止拼接
                if ((i+1)==skuSaleAttrValueListBySpu.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId())){
                    map.put(key,skuSaleAttrValue.getSkuId());
                    // 清空key
                    key="";
                }
            }
        }

        // 将map 转换json
        String valuesSkuJson  = JSON.toJSONString(map);
        //System.out.println(valuesSkuJson);
        request.setAttribute("valuesSkuJson",valuesSkuJson);

        // 保存到作用域
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);
       //保存skuInfo
       request.setAttribute("skuInfo",skuInfo);
        return "item";
    }
}
