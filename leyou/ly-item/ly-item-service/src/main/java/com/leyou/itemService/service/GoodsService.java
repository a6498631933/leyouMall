package com.leyou.itemService.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.item.pojo.Brand;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuBo;
import com.leyou.itemService.mapper.BrandMapper;
import com.leyou.itemService.mapper.SpuMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
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

    public PageInfo<SpuBo> querySpuByPageAndSort(Integer page, Integer rows, String key) {

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

        PageInfo<Spu> tempInfo = new PageInfo<>(pageInfo);
        PageInfo<SpuBo> ret = new PageInfo<>(list);
        ret.setTotal(tempInfo.getTotal());
        return ret;
    }
}
