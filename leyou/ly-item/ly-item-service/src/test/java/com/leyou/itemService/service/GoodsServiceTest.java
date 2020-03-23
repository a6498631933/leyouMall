package com.leyou.itemService.service;

import com.leyou.item.pojo.Spu;
import com.leyou.itemService.LyItemService;
import com.leyou.itemService.mapper.SpuMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = LyItemService.class)
public class GoodsServiceTest {
    @Autowired
    private GoodsService goodsService;

    @Autowired
    private SpuMapper spuMapper;

    @Test
    public void testRabbit() {
      Spu spu = goodsService.querySpuById(207L);
      spu.setSubTitle("puppzzzammm");
      spuMapper.updateByPrimaryKeySelective(spu);
      goodsService.sendMessage(spu.getId(),"update");
    }
}
