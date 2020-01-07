package com.atguigu.gmall0715.list.service.imp;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0715.bean.SkuLsInfo;
import com.atguigu.gmall0715.bean.SkuLsParams;
import com.atguigu.gmall0715.bean.SkuLsResult;
import com.atguigu.gmall0715.config.RedisUtil;
import com.atguigu.gmall0715.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.apache.lucene.util.QueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {
    public static final String ES_INDEX = "gmall";

    public static final String ES_TYPE = "SkuInfo";

    @Autowired
    private JestClient jestClient;
    @Autowired
    private RedisUtil redisUtil;
    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {
         /*
        1.  定义好动作
            put /gmall/SkuInfo/37 { "id":"1001" ,"skuName":"小艾手机"}

         2. 执行
         */

        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        //1.定义好dsl语句
        String query = makeQueryStringForSearch(skuLsParams);
        //2.准备好执行的动作
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();

        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //3.获取结果集
        // 制作返回结果集
        SkuLsResult skuLsResult = makeResultForSearch(searchResult, skuLsParams);
        return skuLsResult;
    }

    @Override
    public void incrHotScore(String skuId) {
        Jedis jedis = redisUtil.getJedis();
        //zSet
        Double hotScore = jedis.zincrby("hotScore", 1, "skuId:" + skuId);
        //当达到一定次数就更新es
        if(hotScore%10==0){
            updateHotScore(skuId,  Math.round(hotScore));
        }
    }

    private void updateHotScore(String skuId, long hotScore) {
        //定义dsl语句
        String upd="{\n" +
                "  \"doc\": {\n" +
                "      \"hotScore\": "+hotScore+"\n" +
                "  }\n" +
                "}";
        //执行动作
        Update update = new Update.Builder(upd).index(ES_INDEX).type(ES_TYPE).id(skuId).build();
        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 返回结果集
     * @param searchResult
     * @param skuLsParams
     * @return
     */
    private SkuLsResult makeResultForSearch(SearchResult searchResult, SkuLsParams skuLsParams) {
        SkuLsResult skuLsResult = new SkuLsResult();
//        List<SkuLsInfo> skuLsInfoList;
        // 保存商品的！ skuLsInfoList
        ArrayList<SkuLsInfo> infoArrayList = new ArrayList<>();
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        if (hits != null && hits.size() > 0) {
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo skuLsInfo = hit.source;
                // 如果走全文检索则skuName不是高亮 ，得到高亮skuName
                if (hit.highlight != null && hit.highlight.size() > 0) {
                    List<String> list = hit.highlight.get("skuName");
                    String skuNameHI = list.get(0);
                    skuLsInfo.setSkuName(skuNameHI);
                }
                infoArrayList.add(skuLsInfo);
            }
        }

        skuLsResult.setSkuLsInfoList(infoArrayList);
//        // 总条数
//        long total;
        skuLsResult.setTotal(searchResult.getTotal());

//        // 总页数
//        long totalPages;
        // 10 3 4  | 9 3 3
        // long totalPages = searchResult.getTotal()%skuLsParams.getPageSize()==0?searchResult.getTotal()/skuLsParams.getPageSize():searchResult.getTotal()/skuLsParams.getPageSize()+1;
        long totalPages = (searchResult.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);
//        // 平台属性值Id集合 显示平台属性，平台属性值
//        List<String> attrValueIdList;
        ArrayList<String> stringArrayList = new ArrayList<>();
        TermsAggregation groupby_attr = searchResult.getAggregations().getTermsAggregation("groupby_attr");
        // 得到集合
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        if (buckets!=null && buckets.size()>0){
            for (TermsAggregation.Entry bucket : buckets) {
                String valueId = bucket.getKey();
                // 将valueId 添加到stringArrayList
                stringArrayList.add(valueId);
            }
        }
        skuLsResult.setAttrValueIdList(stringArrayList);

        return skuLsResult;
    }

    /**
     * 根据用户输入的条件生成dsl语句
     * @param skuLsParams
     * @return
     */
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        //1.定义一个查询器{}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //3.query{bool}
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //2.query
        searchSourceBuilder.query(boolQueryBuilder);
        // 按照三级分类Id 过滤
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {
            // filter --- term {"term": {"catalog3Id": "61"}}
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }

        // 判断平台属性值Id 过滤
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            // 循环  {"term": {"skuAttrValueList.valueId": "82"}}
            for (String valueId : skuLsParams.getValueId()) {
                // 判断平台属性值Id
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        // 判断是否有skuName --- keyword
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0) {
            /*
            {"match": {
              "skuName": "小米三代"
            }}
             */
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            // bool -- must
            boolQueryBuilder.must(matchQueryBuilder);

            // 设置高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<span style=color:red>");
            highlightBuilder.postTags("</span>");
            highlightBuilder.field("skuName");

            searchSourceBuilder.highlight(highlightBuilder);
        }
        // 排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        // 分页
        // 分页公式：10 3   第一页 0,3 第二页3,3 第三页 6,3 第四页 9,3
        int from = skuLsParams.getPageSize() * (skuLsParams.getPageNo() - 1);
        searchSourceBuilder.from(from);
        // 默认每页显示20条数据
        searchSourceBuilder.size(skuLsParams.getPageSize());
        // 聚合
        /*
        "aggs": {
            "groupby_attr": {
              "terms": {
                "field": "skuAttrValueList.valueId"
              }
            }
          }
         */
        // 如果说执行的时候报错：fieldData skuAttrValueList.valueId 设置true   skuAttrValueList.valueId --> skuAttrValueList.valueId.keyword
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);

        // { query }  {bool 放入query}
        searchSourceBuilder.query(boolQueryBuilder);

        String query = searchSourceBuilder.toString();
        System.out.println("query" + query);
        return query;
    }
}