package com.atguigu.gmall0715.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0715.bean.*;
import com.atguigu.gmall0715.config.RedisUtil;
import com.atguigu.gmall0715.manage.constant.ManageConst;
import com.atguigu.gmall0715.manage.mapper.*;
import com.atguigu.gmall0715.service.ManageService;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ManageServiceImpl implements ManageService{
    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;
    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;
    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;
    //从spring容器中获取数据
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<BaseCatalog1> getCatalog1() {

        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(BaseCatalog2 baseCatalog2) {

        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(BaseCatalog3 baseCatalog3) {

        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(BaseAttrInfo baseAttrInfo) {
        return baseAttrInfoMapper.select(baseAttrInfo);
    }
    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        if(baseAttrInfo.getId()!=null&&baseAttrInfo.getId().length()>0){
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        }else {
            //直接保存平台属性
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }
        // baseAttrValue 修改：
        // 先将原有的数据删除，然后再新增！
        BaseAttrValue baseAttrValueDel= new BaseAttrValue();
        baseAttrValueDel.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValueDel);
        //保存平台属性值
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        //判断对象不为空，然后再判断集合长度必须大于0
        if (attrValueList!=null && attrValueList.size()>0){
            for (BaseAttrValue baseAttrValue : attrValueList) {
                // 平台属性值Id主键自增，平台属性Id baseAttrValue.attrId = baseAttrInfo.id
                baseAttrValue.setAttrId(baseAttrInfo.getId()); // 获取当前对象主键自增值！
                baseAttrValueMapper.insertSelective(baseAttrValue);
            }
        }
    }

    @Override
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        return baseAttrValueMapper.select(baseAttrValue);
    }

    @Override
    public BaseAttrInfo getBaseAttrInfo(String attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        // 查询平台属性值集合
        baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        return baseAttrInfo;
    }

    @Override
    public List<SpuInfo> getSpuInfoList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {

        return baseSaleAttrMapper.selectAll();
    }
    @Transactional
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        /*
            spuInfo
            spuImage
            spuSaleAttr
            spuSaleAttrValue
         */
        spuInfoMapper.insertSelective(spuInfo);
        // spuImage
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (spuImageList!=null && spuImageList.size()>0){
            for (SpuImage spuImage : spuImageList) {
                // 赋值spuId
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }
        // 销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (spuSaleAttrList!=null && spuSaleAttrList.size()>0){
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);

                // 销售属性值
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList!=null && spuSaleAttrValueList.size()>0){
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(SpuImage spuImage) {
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(String catalog3Id) {
        return baseAttrInfoMapper.selectBaseAttrInfoListByCatalog3Id(catalog3Id);
    }

    @Override
    public List<SpuSaleAttr> getspuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }
    @Transactional
    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        //直接保存商品
        skuInfoMapper.insert(skuInfo);
        //保存图片

        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList!=null && skuImageList.size()>0){
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(skuImage);
            }
        }

        // 获取出sku与平台属性的关系
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList!=null&& skuAttrValueList.size()>0){
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(skuAttrValue);
            }
        }
        // 获取sku与销售属性的集合
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList!=null && skuSaleAttrValueList.size()>0){
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                // skuId 赋值
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(skuSaleAttrValue);
            }
        }
    }
    //将数据放入缓存
    @Override
    public SkuInfo getSkuInfo(String skuId) {
        //return getSkuInfoByRedisSet(skuId);
        return getSkuInfoRedisson(skuId);

    }

    private SkuInfo getSkuInfoRedisson(String skuId) {
        SkuInfo skuInfo = null;
        Jedis jedis = null;
        try {
            // 获取Jedis
            jedis = redisUtil.getJedis();
            // 定义key
            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;

            String skuJson = jedis.get(skuKey);

            if(skuJson==null){
                // redisson 加锁，走数据库并放入缓存！
                Config config = new Config();
                // 设置redis 节点
                config.useSingleServer().setAddress("redis://192.168.1.125:6379");

                // 创建Redisson 实例
                RedissonClient redisson = Redisson.create(config);
                // 创建锁
                RLock lock = redisson.getLock("myLock");
                System.out.println("redisson 分布式锁！");
                // lock.lock();  set key,vlaue,px,10000,nx
                // lock.lock(10, TimeUnit.SECONDS);
                boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
                if (res) {
                    try {
                        // 业务逻辑
                        // 缓存中没有数据
                        skuInfo =  getSkuInfoDB(skuId);
                        if(skuInfo==null){
                            jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,"");
                        }
                        // 将数据放入缓存
                        jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
                        return skuInfo;
                    } finally {
                        // 解锁！
                        lock.unlock();
                    }
                }
            }else {
                // 缓存有数据
                // skuJson -- 转换为对象
                skuInfo = JSON.parseObject(skuJson,SkuInfo.class);
                return skuInfo;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭缓存
            if (jedis!=null){
                jedis.close();
            }
        }
        return getSkuInfoDB(skuId);
    }

    private SkuInfo getSkuInfoByRedisSet(String skuId) {
        SkuInfo skuInfo = null;
        Jedis jedis = null;
        try {
            // 获取Jedis、
            jedis = redisUtil.getJedis();
            // 定义key
            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;

            String skuJson = jedis.get(skuKey);
            if (skuJson==null){
                // 没有数据
                System.out.println("缓存中没有数据：");
                // 准备加锁！  set k1 v1 px 10000 nx  解决缓存击穿的问题
                // 定义锁的key sku:skuId:lock  k1
                String skuLockKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKULOCK_SUFFIX;

                // 定义key 锁定的值  v1
                String token = UUID.randomUUID().toString().replace("-","");

                // 执行加锁命令
                String lockKey = jedis.set(skuLockKey, token, "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);
                if ("OK".equals(lockKey)){
                    System.out.println("上锁成功！");
                    // 从db 中数据并放入缓存！
                    // 缓存中没有数据
                    skuInfo =  getSkuInfoDB(skuId);
                    // 将数据放入缓存
                    jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));

                    // 解锁：
                    // jedis.del(skuKey); lua 脚本：
                    String script ="if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                    jedis.eval(script, Collections.singletonList(skuLockKey),Collections.singletonList(token));

                    return skuInfo;
                }else {
                    // 说明里面有人！等待
                    Thread.sleep(1000);

                    return getSkuInfo(skuId);
                }

            }else {
                // 缓存中有数据！
                // skuJson -- 转换为对象
                skuInfo = JSON.parseObject(skuJson,SkuInfo.class);
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭缓存
            if (jedis!=null){
                jedis.close();
            }
        }

        return getSkuInfoDB(skuId);
    }

    //抽取方法单独走数据库
    private SkuInfo getSkuInfoDB(String skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        // select * from skuImage where skuId = skuId
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        skuInfo.setSkuImageList( skuImageMapper.select(skuImage));
        // skuAttrValue
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        skuInfo.setSkuAttrValueList(skuAttrValueMapper.select(skuAttrValue));
        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(),skuInfo.getSpuId());
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }
}
