package com.leyou.item.pojo;

import java.util.ArrayList;
import java.util.List;

public class SpecParam {
    String k;
    Boolean searchable;
    Boolean global;
    List<String> opthions = new ArrayList<>();

    public String getK() {
        return k;
    }

    public void setK(String k) {
        this.k = k;
    }

    public Boolean getSearchable() {
        return searchable;
    }

    public void setSearchable(Boolean searchable) {
        this.searchable = searchable;
    }

    public Boolean getGlobal() {
        return global;
    }

    public void setGlobal(Boolean global) {
        this.global = global;
    }

    public List<String> getOpthions() {
        return opthions;
    }

    public void setOpthions(List<String> opthions) {
        this.opthions = opthions;
    }

    public void addOptions(String  option) {
        this.opthions.add(option);
    }
}
