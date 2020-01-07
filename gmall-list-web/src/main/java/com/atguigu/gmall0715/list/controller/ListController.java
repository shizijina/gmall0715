package com.atguigu.gmall0715.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0715.bean.*;
import com.atguigu.gmall0715.service.ListService;
import com.atguigu.gmall0715.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {
    @Reference
    private ListService listService;
    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
    //@ResponseBody
    public String list(SkuLsParams skuLsParams, HttpServletRequest request){
        //显示每页的条数
        skuLsParams.setPageSize(3);

        SkuLsResult skuLsResult = listService.search(skuLsParams);
       // return JSON.toJSONString(skuLsResult);
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        //显示平台属性，平台属性值
        //必须的得到平台属性id
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        //根据平台属性值id查询平台属性集合
        List<BaseAttrInfo> baseAttrInfoList=manageService.getAttrInfoList(attrValueIdList);
        //制作url参数
        String urlParam=makeUrlParam(skuLsParams);
       // System.out.println("查询参数列表"+urlParam);
        // 声明一个保存面包屑的集合
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();
        if(baseAttrInfoList!=null&&baseAttrInfoList.size()>0){
            for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
                BaseAttrInfo baseAttrInfo = iterator.next();
                //获取平台属性集合
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                for (BaseAttrValue baseAttrValue : attrValueList) {
                    if (skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
                        for (String valueId : skuLsParams.getValueId()) {
                            if(baseAttrValue.getId().equals(valueId)){
                                //删除baseAttrInfo
                                iterator.remove();
                                // 组装面包屑 平台属性名称：平台属性值名称
                                // 将面包屑的内容 赋值给了平台属性值对象的名称
                                BaseAttrValue baseAttrValueed = new BaseAttrValue();
                                baseAttrValueed.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                                String newUrlParam = makeUrlParam(skuLsParams, valueId);
                                //赋值最新的参数列表
                                baseAttrValueed.setUrlParam(newUrlParam);
                                // 将每个面包屑都放入集合中！
                                baseAttrValueArrayList.add(baseAttrValueed);
                            }
                        }
                    }
                }
            }
        }
        // 分页：
        request.setAttribute("pageNo",skuLsParams.getPageNo());
        request.setAttribute("totalPages",skuLsResult.getTotalPages());

        //保存数据
        request.setAttribute("keyword",skuLsParams.getKeyword());//获取keyword
        request.setAttribute("baseAttrValueArrayList",baseAttrValueArrayList);
        request.setAttribute("urlParam",urlParam);
        request.setAttribute("baseAttrInfoList",baseAttrInfoList);
        request.setAttribute("skuLsInfoList",skuLsInfoList);
        return "list";
    }
    //制作查询参数
    private String makeUrlParam(SkuLsParams skuLsParams,String... excludeValueIds) {
        String urlParam="";
        //判断用户是否输入keyword
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            urlParam+="keyword="+skuLsParams.getKeyword();
        }
        // 判断用户是否输入的三级分类Id
        // http://list.gmall.com/list.html?catalog3Id=61
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }
        //判断用户是否输入平台id
        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
            for (String valueId : skuLsParams.getValueId()) {
                if(excludeValueIds!=null&&excludeValueIds.length>0){
                    //获取对象中的第一个数据
                    String excludeValueId = excludeValueIds[0];
                    if(excludeValueId.equals(valueId)){
                        continue;
                    }
                }
                urlParam+="&valueId="+valueId;
            }
        }
            return urlParam;
    }
}
