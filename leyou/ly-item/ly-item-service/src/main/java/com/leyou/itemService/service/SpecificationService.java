package com.leyou.itemService.service;

import com.leyou.item.pojo.Specification;
import com.leyou.itemService.mapper.SpecificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SpecificationService {

    @Autowired
    SpecificationMapper specificationMapper;

    public Specification queryById(Long id) {
        return specificationMapper.selectByPrimaryKey(id);
    }
}
