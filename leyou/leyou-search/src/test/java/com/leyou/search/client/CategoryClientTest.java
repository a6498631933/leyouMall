package com.leyou.search.client;

import com.leyou.search.LySearchService;
import com.leyou.search.pojo.Goods;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LySearchService.class)
public class CategoryClientTest {

    @Autowired
    private CategoryClient categoryClient;

    @Test
    public void testQueryCategories() {
        List<Long> ids = new  ArrayList<>();
        ids.add(1L);
        ids.add(2L);
        ids.add(3L);
        List<String> names = this.categoryClient.queryNameByIds(ids);
        names.forEach(System.out::println);
    }
}