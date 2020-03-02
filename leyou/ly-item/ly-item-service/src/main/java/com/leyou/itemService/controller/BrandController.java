package com.leyou.itemService.controller;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageInfo;
import com.leyou.item.pojo.Brand;
import com.leyou.itemService.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("brand")
public class BrandController {

    @Autowired
    BrandService brandService;

    @GetMapping("/page")
    public ResponseEntity<PageInfo<Brand>> queryBrandByPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "desc", defaultValue = "false") Boolean desc,
            @RequestParam(value = "key", required = false) String key){
        PageInfo<Brand> resultPage = brandService.queryBrandByPageAndSort(page, rows, sortBy, desc, key);
        if (resultPage == null || resultPage.getPageSize() == 0) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(resultPage);
    }

    @PostMapping
    public ResponseEntity<Void> saveBrand(Brand brand, @RequestParam("cids") List<Long> cids) {
        this.brandService.saveBrand(brand, cids);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
}
