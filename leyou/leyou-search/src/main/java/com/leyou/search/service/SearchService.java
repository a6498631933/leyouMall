package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.respository.GoodsRepository;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    private ObjectMapper mapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);


    public Goods buildGoods(Spu spu) throws IOException {

        Goods goods = new Goods();

        //1.查询商品分类名称
        List<String> names = this.categoryClient.queryNameByIds(Arrays.asList(spu.getCid1(),spu.getCid2(),spu.getCid3()));
        //2.查询sku
        List<Sku> skus = this.goodsClient.querySkuBySpuId(spu.getId());
        //3.查询详情
        SpuDetail spuDetail = this.goodsClient.querySpuDetailById(spu.getId());

        //4.处理sku,仅封装id，价格、标题、图片、并获得价格集合

        List<Long> prices = new ArrayList<>();
        List<Map<String,Object>> skuLists = new ArrayList<>();
        skus.forEach(sku -> {
            prices.add(sku.getPrice());
            Map<String,Object> skuMap = new HashMap<>();
            skuMap.put("id",sku.getId());
            skuMap.put("title",sku.getTitle());
            skuMap.put("price",sku.getPrice());
            //取第一张图片
            skuMap.put("image", StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(),",")[0]);
            skuLists.add(skuMap);
        });

        //提取公共属性
        List<Map<String,Object>> genericSpecs = mapper.readValue(spuDetail.getSpecifications(),new TypeReference<List<Map<String,Object>>>(){});
        //过滤规格模板，把所有可搜索的信息保存到Map中
        Map<String,Object> specMap = new HashMap<>();

        String searchable = "searchable";
        String v = "v";
        String k = "k";
        String options = "options";

        genericSpecs.forEach(m -> {
            List<Map<String, Object>> params = (List<Map<String, Object>>) m.get("params");
            params.forEach(spe ->{
                if ((boolean)spe.get(searchable)){
                    if (spe.get(v) != null){
                        specMap.put(spe.get(k).toString(), spe.get(v).toString());
                    }else if (spe.get(options) != null){
                        specMap.put(spe.get(k).toString(), spe.get(options).toString());
                    }
                }
            });
        });
        goods.setId(spu.getId());
        goods.setSubTitle(spu.getSubTitle());
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setAll(spu.getTitle() + " " + StringUtils.join(names, " "));
        goods.setPrice(prices);
        goods.setSkus(mapper.writeValueAsString(skuLists));
        goods.setSpecs(specMap);
        return goods;
    }


    public SearchResult search(SearchRequest request) {
        String key = request.getKey();
        // 判断是否有搜索条件，如果没有，直接返回null。不允许搜索全部商品
        if (StringUtils.isBlank(key)) {
            return null;
        }
        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        MatchQueryBuilder basicQuery = QueryBuilders.matchQuery("all", key).operator(Operator.AND);
        queryBuilder.withQuery(basicQuery);
        // 通过sourceFilter设置返回的结果字段,我们只需要id、skus、subTitle
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "skus", "subTitle"}, null));

        // 分页
        searchWithPageAndSort(queryBuilder, request);

        // 聚合
        queryBuilder.addAggregation(AggregationBuilders.terms("category").field("cid3"));
        queryBuilder.addAggregation(AggregationBuilders.terms("brand").field("brandId"));

        // 执行查询获取结果集
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>) this.goodsRepository.search(queryBuilder.build());

        // 获取聚合结果集
        // 商品分类的聚合结果
        List<Category> categories =
                getCategoryAggResult(goodsPage.getAggregation("category"));
        // 品牌的聚合结果
        List<Brand> brands = getBrandAggResult(goodsPage.getAggregation("brand"));

        // 根据商品分类判断是否需要聚合
        List<Map<String, Object>> specs = new ArrayList<>();
        if (categories.size() == 2) {
            // 如果商品分类只有一个才进行聚合，并根据分类与基本查询条件聚合
            specs = getSpec(categories.get(0).getId(), basicQuery);
        }

        return new SearchResult(goodsPage.getTotalElements(), goodsPage.getTotalPages(), goodsPage.getContent(), categories, brands, specs);
    }



    private List<Map<String, Object>> getSpec(Long id, QueryBuilder query) {
        try {
            // 不管是全局参数还是sku参数，只要是搜索参数，都根据分类id查询出来
            List<SpecParam> params = this.specificationClient.querySpecParamsByID(id);
            List<Map<String, Object>> specs = new ArrayList<>();

            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
            queryBuilder.withQuery(query);

            // 聚合规格参数
            params.forEach(p -> {
                String key = p.getK();
                queryBuilder.addAggregation(AggregationBuilders.terms(key).field("specs." + key + ".keyword"));
            });

            // 查询
            queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{}, null));

            AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>)this.goodsRepository.search(queryBuilder.build());

            // 解析聚合结果
//            params.forEach(param -> {
//                String key = param.getK();
//                spec.put("k", key);
//                StringTerms terms = (StringTerms) aggs.get(key);
//                if (param.getOpthions().size() >= 2) {
//                    Map<String, Object> spec = new HashMap<>();
//                    spec.put("k", param.getK());
//                    spec.put("options", param.getOpthions());
//                    specs.add(spec);
//                }
//            });

             Map<String, Aggregation> aggregationMap = goodsPage.getAggregations().asMap();
             for(Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
                 Map<String, Object> map = new HashMap<>();
                 map.put("k", entry.getKey());
                 System.out.println("*******************"  + entry.getKey());

                 if(entry.getValue() instanceof StringTerms){
                     StringTerms terms = (StringTerms) entry.getValue();

                     List<String> options = new ArrayList<>();
                     terms.getBuckets().forEach(bucket -> {
                         options.add(bucket.getKeyAsString());
                     });
                     map.put("options", options);
                     specs.add(map);
                 }
             }

            return specs;
        } catch (
                Exception e)

        {
            logger.error("规格聚合出现异常：", e);
            return null;
        }


    }

    // 构建基本查询条件
    private void searchWithPageAndSort(NativeSearchQueryBuilder queryBuilder, SearchRequest request) {
        // 准备分页参数
        int page = request.getPage();
        int size = request.getSize();

        // 1、分页
        queryBuilder.withPageable(PageRequest.of(page - 1, size));
        // 2、排序
        String sortBy = request.getSortBy();
        Boolean desc = request.getDescending();
        if (StringUtils.isNotBlank(sortBy)) {
            // 如果不为空，则进行排序
            queryBuilder.withSort(SortBuilders.fieldSort(sortBy).order(desc ? SortOrder.DESC : SortOrder.ASC));
        }
    }

    // 解析品牌聚合结果
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        try {
            LongTerms brandAgg = (LongTerms) aggregation;
            List<Long> bids = new ArrayList<>();
            for (LongTerms.Bucket bucket : brandAgg.getBuckets()) {
                bids.add(bucket.getKeyAsNumber().longValue());
            }
            // 根据id查询品牌
            return this.brandClient.queryBrandByIds(bids);
        } catch (Exception e){
            logger.error("品牌聚合出现异常：", e);
            return null;
        }
    }



    // 解析商品分类聚合结果
    private List<Category> getCategoryAggResult(Aggregation aggregation) {
        try{
            List<Category> categories = new ArrayList<>();
            LongTerms categoryAgg = (LongTerms) aggregation;
            List<Long> cids = new ArrayList<>();
            for (LongTerms.Bucket bucket : categoryAgg.getBuckets()) {
                cids.add(bucket.getKeyAsNumber().longValue());
            }
            // 根据id查询分类名称
            List<String> names = this.categoryClient.queryNameByIds(cids);

            for (int i = 0; i < names.size(); i++) {
                Category c = new Category();
                c.setId(cids.get(i));
                c.setName(names.get(i));
                categories.add(c);
            }
            return categories;
        } catch (Exception e){
            logger.error("分类聚合出现异常：", e);
            return null;
        }
    }
}