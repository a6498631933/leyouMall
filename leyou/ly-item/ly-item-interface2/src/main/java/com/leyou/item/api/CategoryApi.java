package com.leyou.item.api;

import com.leyou.item.pojo.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequestMapping("category")
public interface CategoryApi {
    @GetMapping("names")
    List<String> queryNameByIds(@RequestParam("ids") List<Long> ids);


    @GetMapping("all/level")
    public List<Category> queryAllByCid3(@RequestParam("id") Long id);
}
