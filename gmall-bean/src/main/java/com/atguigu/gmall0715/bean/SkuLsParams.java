package com.atguigu.gmall0715.bean;

import lombok.Data;

import java.io.Serializable;
@Data
public class SkuLsParams implements Serializable {
    //skuName
    String  keyword;

    String catalog3Id;
    //平台属性值id
    String[] valueId;

    int pageNo=1;

    int pageSize=20;
}
