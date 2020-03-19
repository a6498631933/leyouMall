package com.leyou.itemService.controller;

import com.leyou.item.pojo.SpecParam;
import com.leyou.item.pojo.Specification;
import com.leyou.itemService.service.SpecificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("spec")
public class SpecificationController {

    @Autowired
    private SpecificationService specificationService;

    @GetMapping("/{id}")
    public ResponseEntity<String> querySpecificationByCategoryId(@PathVariable("id") Long id) {
        Specification spec = specificationService.queryById(id);
        if(spec == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(spec.getSpecifications());
    }

    @GetMapping("params/{cid}")
    public ResponseEntity<List<SpecParam>> querySpecParamsByID(@PathVariable("cid") Long id){
//        return specificationService.queryParamsById(id);

        List<SpecParam> spec = specificationService.queryParamsById(id);
        if(spec == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(spec);
    }
}
