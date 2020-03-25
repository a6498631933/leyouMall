package com.leyou.itemService.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.pojo.*;
import com.leyou.itemService.mapper.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    private static final Logger logger = LoggerFactory.getLogger(GoodsService.class);

    public PageResult<SpuBo> querySpuByPageAndSort(Integer page, Integer rows, String key) {

        // 1、查询SPU
        // 分页,最多允许查100条
        PageHelper.startPage(page, Math.min(rows, 100));
        //创建条件查询
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
//        // 是否过滤上下架
//        if(saleable != null) {
//            criteria.orEqualTo("saleabble", saleable);
//        }
        //判断模糊查询
        if(StringUtils.isNotBlank(key)){
            criteria.andLike("title", "%" + key + "%");
        }
        Page<Spu> pageInfo = (Page<Spu>)this.spuMapper.selectByExample(example);

        List<SpuBo> list = pageInfo.getResult().stream().map(spu -> {
            // 2、把spu变为 spuBo
            SpuBo spuBo = new SpuBo();

            // 属性拷贝
            BeanUtils.copyProperties(spu, spuBo);

            // 3、查询spu的商品分类名称,要查三级分类
            List<String> names = this.categoryService.queryNameByIds(
                    Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

            // 将分类名称拼接后存入
            spuBo.setCname(StringUtils.join(names, "/"));

            // 4、查询spu的品牌名称
            Brand brand = this.brandMapper.selectByPrimaryKey(spu.getBrandId());
            spuBo.setBname(brand.getName());
            return spuBo;

        }).collect(Collectors.toList());

//        PageInfo<Spu> tempInfo = new PageInfo<>(pageInfo);
//        PageInfo<SpuBo> ret = new PageInfo<>(list);
//        ret.setTotal(tempInfo.getTotal());
        return new PageResult<>(pageInfo.getTotal() ,list);
    }

    @Transactional
    public void save(SpuBo spu) {
        // 保存spu
        spu.setSaleable(true);
        spu.setValid(true);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        this.spuMapper.insert(spu);
        // 保存spu详情
        spu.getSpuDetail().setSpuId(spu.getId());
        this.spuDetailMapper.insert(spu.getSpuDetail());

        // 保存sku和库存信息
        saveSkuAndStock(spu.getSkus(), spu.getId());

        sendMessage(spu.getId(), "insert");
    }

    /**
     *
     * @param spu
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(SpuBo spu) {
        /**
         * 更新策略：
         *      1.判断tb_spu_detail中的spec_template字段新旧是否一致
         *      2.如果一致说明修改的只是库存、价格和是否启用，那么就使用update
         *      3.如果不一致，说明修改了特有属性，那么需要把原来的sku全部删除，然后添加新的sku
         */

        //更新spu
        spu.setSaleable(true);
        spu.setValid(true);
        spu.setLastUpdateTime(new Date());
        this.spuMapper.updateByPrimaryKeySelective(spu);

        //更新spu详情
        SpuDetail spuDetail = spu.getSpuDetail();
        String oldTemp = this.spuDetailMapper.selectByPrimaryKey(spu.getId()).getSpecTemplate();
        if(spuDetail.getSpecTemplate().equals(oldTemp)){
            //相等， sku update
            //更新sku和库存
            updateSkuAndStock(spu.getSkus(),spu.getId(),true);
        }else {
            //不等，sku insert
            //更新sku和库存信息
            updateSkuAndStock(spu.getSkus(),spu.getId(),false);
        }
        spuDetail.setSpuId(spu.getId());
        this.spuDetailMapper.updateByPrimaryKeySelective(spuDetail);

        //发送消息到mq
        this.sendMessage(spu.getId(),"update");
    }

    private void saveSkuAndStock(List<Sku> skus, Long spuId) {
        for (Sku sku : skus) {
            if (!sku.getEnable()) {
                continue;
            }
            // 保存sku
            sku.setSpuId(spuId);
            // 默认不参与任何促销
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            this.skuMapper.insert(sku);

            // 保存库存信息
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(Integer.parseInt(sku.getStock().toString()));
            this.stockMapper.insert(stock);
//            this.sendMessage();
        }
    }

    private void updateSkuAndStock(List<Sku> skus,Long id,boolean tag) {
        //通过tag判断是insert还是update
        //获取当前数据库中spu_id = id的sku信息
        Example e = new Example(Sku.class);
        e.createCriteria().andEqualTo("spuId",id);
        //oldList中保存数据库中spu_id = id 的全部sku
        List<Sku> oldList = this.skuMapper.selectByExample(e);
        if (tag){
            /**
             * 判断是更新时是否有新的sku被添加：如果对已有数据更新的话，则此时oldList中的数据和skus中的ownSpec是相同的，否则则需要新增
             */
            int count = 0;
            for (Sku sku : skus){
                if (!sku.getEnable()){
                    continue;
                }
                for (Sku old : oldList){
                    if (sku.getOwnSpec().equals(old.getOwnSpec())){
                        System.out.println("更新");
                        //更新
                        List<Sku> list = this.skuMapper.select(old);
                        if (sku.getPrice() == null){
                            sku.setPrice(0L);
                        }
                        if (sku.getStock() == null){
                            sku.setStock(0L);
                        }
                        sku.setId(list.get(0).getId());
                        sku.setCreateTime(list.get(0).getCreateTime());
                        sku.setSpuId(list.get(0).getSpuId());
                        sku.setLastUpdateTime(new Date());
                        this.skuMapper.updateByPrimaryKey(sku);
                        //更新库存信息
                        Stock stock = new Stock();
                        stock.setSkuId(sku.getId());
                        stock.setStock(Integer.parseInt(sku.getStock().toString()));
                        this.stockMapper.updateByPrimaryKeySelective(stock);
                        //从oldList中将更新完的数据删除
                        oldList.remove(old);
                        break;
                    }else{
                        //新增
                        count ++ ;
                    }
                }
                if (count == oldList.size() && count != 0){
                    //当只有一个sku时，更新完因为从oldList中将其移除，所以长度变为0，所以要需要加不为0的条件
                    List<Sku> addSku = new ArrayList<>();
                    addSku.add(sku);
                    saveSkuAndStock(addSku,id);
                    count = 0;
                }else {
                    count =0;
                }
            }
            //处理脏数据
            if (oldList.size() != 0){
                for (Sku sku : oldList){
                    this.skuMapper.deleteByPrimaryKey(sku.getId());
                    Example example = new Example(Stock.class);
                    example.createCriteria().andEqualTo("skuId",sku.getId());
                    this.stockMapper.deleteByExample(example);
                }
            }
        }else {
            List<Long> ids = oldList.stream().map(Sku::getId).collect(Collectors.toList());
            //删除以前的库存
            Example example = new Example(Stock.class);
            example.createCriteria().andIn("skuId",ids);
            this.stockMapper.deleteByExample(example);
            //删除以前的sku
            Example example1 = new Example(Sku.class);
            example1.createCriteria().andEqualTo("spuId",id);
            this.skuMapper.deleteByExample(example1);
            //新增sku和库存
            saveSkuAndStock(skus,id);
        }

    }

    public SpuDetail querySpuDetailById(Long id) {
        return spuDetailMapper.selectByPrimaryKey(id);
    }

    public List<Sku> querySkuBySpuId(Long id) {
        // 查询sku
        Sku record = new Sku();
        record.setSpuId(id);
        List<Sku> skus = this.skuMapper.select(record);
        for (Sku sku : skus) {
            // 同时查询出库存
            Integer stock = this.stockMapper.selectByPrimaryKey(sku.getId()).getStock();
            sku.setStock(stock.longValue());
        }
        return skus;
    }

    public Spu querySpuById(Long id) {
        return this.spuMapper.selectByPrimaryKey(id);
    }

    public void sendMessage(Long id, String type){
        //发送消息
        try {
            this.amqpTemplate.convertAndSend("item." + type, id);
        }
        catch (Exception e){
            logger.error("{}商品消息发送异常，商品id：{}", type, id, e);
        }
    }

    public Sku querySkuById(Long id) {
        return this.skuMapper.selectByPrimaryKey(id);
    }
}
