package com.leyou.item.api;

import com.leyou.item.pojo.SpecParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("spec")
public interface SpecificationApi {
    @GetMapping("/{id}")
    public String querySpecificationByCategoryId(@PathVariable("id") Long id);

    @GetMapping("params/{cid}")
    public List<SpecParam> querySpecParamsByID(@PathVariable("cid") Long id);
}
