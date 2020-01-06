package com.atguigu.gmall0715.service;

import com.atguigu.gmall0715.bean.SkuLsInfo;
import com.atguigu.gmall0715.bean.SkuLsParams;
import com.atguigu.gmall0715.bean.SkuLsResult;

public interface ListService {
    /**
     * 商品上架
     */
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    /**
     * 根据用户输入的条件查询数据
     * @param skuLsParams
     * @return
     */
    SkuLsResult search (SkuLsParams skuLsParams);
}
