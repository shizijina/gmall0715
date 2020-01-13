package com.atguigu.gmall0715.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall0715.bean.PaymentInfo;
import com.atguigu.gmall0715.bean.enums.PaymentStatus;
import com.atguigu.gmall0715.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall0715.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.HashMap;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private AlipayClient alipayClient;
    @Override
    public void savyPaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd) {
        // update paymentInfo set payment_status = PAID where out_trade_no = ?
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",out_trade_no);
        paymentInfoMapper.updateByExampleSelective(paymentInfoUpd,example);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {
        return paymentInfoMapper.selectOne(paymentInfo);
    }

    @Override
    public boolean refund(String orderId) {

        // 通过订单Id 查询交易记录对象
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        PaymentInfo paymentInfoQuery = getPaymentInfo(paymentInfo);
        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        // 封装业务参数
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",paymentInfoQuery.getOutTradeNo());
        map.put("refund_amount",paymentInfoQuery.getTotalAmount());
        map.put("refund_reason","过年没钱了");

        // json
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            // 交易记录要更改，
            PaymentInfo paymentInfoUpd = new PaymentInfo();
            paymentInfoUpd.setPaymentStatus(PaymentStatus.ClOSED);
            updatePaymentInfo(paymentInfoQuery.getOutTradeNo(),paymentInfoUpd);

            // 订单状态
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }
}
