package com.atguigu.gmall0715.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.List;

@Data
public class SpuSaleAttr implements Serializable {
    @Id
    @Column
    String id ;

    @Column
    String spuId;

    @Column
    String saleAttrId;

    @Column
    String saleAttrName;

    // 销售属性：销售属性值   1：n
    // 销售属性值集合
    @Transient
    List<SpuSaleAttrValue> spuSaleAttrValueList;
}
