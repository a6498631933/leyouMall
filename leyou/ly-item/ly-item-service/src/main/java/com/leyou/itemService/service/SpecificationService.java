package com.leyou.itemService.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.pojo.Specification;
import com.leyou.itemService.mapper.SpecificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SpecificationService {

    @Autowired
    SpecificationMapper specificationMapper;

    public Specification queryById(Long id) {
        return specificationMapper.selectByPrimaryKey(id);
    }

    public List<SpecParam> queryParamsById(Long id) {
        Specification specification = specificationMapper.selectByPrimaryKey(id);
        String spec = specification.getSpecifications();
        JSONArray jsStr = (JSONArray) JSONArray.parse(spec); //将字符串{“id”：1}
        List<SpecParam> specParams = new ArrayList<>();
        jsStr.forEach((obj)->{
            JSONObject jsObj = (JSONObject) obj;
            JSONArray params = jsObj.getJSONArray("params");
            params.forEach(param -> {
                JSONObject JsParam = (JSONObject) param;
                SpecParam specParam = new SpecParam();
                specParam.setK(JsParam.get("k").toString());
                specParam.setSearchable(Boolean.valueOf(JsParam.get("searchable").toString()));
                specParam.setGlobal(Boolean.valueOf(JsParam.get("global").toString()));
                JSONArray options = JsParam.getJSONArray("options");
//                specParam.getOpthions().clear();
                options.forEach(option -> {
                    specParam.addOptions((String)option);
                });
                specParams.add(specParam);
            });
        });


        return specParams;
    }
}
