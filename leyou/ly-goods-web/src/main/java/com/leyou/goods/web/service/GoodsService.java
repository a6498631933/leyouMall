package com.leyou.goods.web.service;

import com.leyou.goods.web.client.BrandClient;
import com.leyou.goods.web.client.CategoryClient;
import com.leyou.goods.web.client.GoodsClient;
import com.leyou.goods.web.client.SpecificationClient;
import com.leyou.item.pojo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoodsService {

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private SpecificationClient specificationClient;

    private static final Logger logger = LoggerFactory.getLogger(GoodsService.class);

    public Map<String, Object> loadModel(Long spuId){

        try {
            // 查询spu
            Spu spu = this.goodsClient.querySpuById(spuId);

            // 查询spu详情
            SpuDetail spuDetail = this.goodsClient.querySpuDetailById(spuId);

            // 查询sku
            List<Sku> skus = this.goodsClient.querySkuBySpuId(spuId);

            // 查询品牌
            List<Brand> brands = this.brandClient.queryBrandByIds(Arrays.asList(spu.getBrandId()));

            // 查询分类
            List<Category> categories = this.categoryClient.queryAllByCid3(spu.getCid3());

//            // 查询组内参数
//            List<SpecGroup> specGroups = this.specificationClient.querySpecsByCid(spu.getCid3());
//
//            // 查询所有特有规格参数
//            List<SpecParam> specParams = this.specificationClient.querySpecParam(null, spu.getCid3(), null, false);
//            // 处理规格参数f
//            Map<Long, String> paramMap = new HashMap<>();
//            specParams.forEach(param->{
//                paramMap.put(param.getId(), param.getName());
//            });

            Map<String, Object> map = new HashMap<>();
            map.put("spu", spu);
            map.put("spuDetail", spuDetail);
            map.put("skus", skus);
            map.put("brand", brands.get(0));
            map.put("categories", categories);
//            map.put("groups", specGroups);
//            map.put("params", paramMap);
            return map;
        } catch (Exception e) {
            logger.error("查询商品分类出错， spuId: {}", spuId, e);
        }

        return null;
    }
}
