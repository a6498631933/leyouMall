package com.leyou.itemService.service;

import com.leyou.item.pojo.Category;
import com.leyou.itemService.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    @Autowired
    private CategoryMapper categoryMapper;

    public List<Category> queryListByParentId(Long pid) {
        Category category = new Category();
        category.setParentId(pid);
        return this.categoryMapper.select(category);
    }

    public List<Category> queryByBrandId(Long bid) {
        return this.categoryMapper.queryByBrandId(bid);
    }

    public List<String> queryNameByIds(List<Long> ids) {
        return this.categoryMapper.selectByIdList(ids).stream().map(Category::getName).collect(Collectors.toList());
    }

    public List<Category> queryAllByCid3(Long id) {
        Category c3 = this.categoryMapper.selectByPrimaryKey(id);
        Category c2 = this.categoryMapper.selectByPrimaryKey(c3.getParentId());
        Category c1 = this.categoryMapper.selectByPrimaryKey(c2.getParentId());
        return Arrays.asList(c1,c2,c3);
    }
}
