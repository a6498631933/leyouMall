package com.leyou.search.pojo;

import com.sun.org.apache.xpath.internal.operations.Bool;

public class SearchRequest {
    private String key;

    private Integer page;

    private String sortBy;

    private Boolean descending;

    private static final Integer DEFAULT_SIZE = 20;
    private static final Integer DEFAULT_PAGE = 1;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getPage() {
        if(page == null) {
            return DEFAULT_PAGE;
        }
        return Math.max(page, DEFAULT_PAGE);
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public static Integer getSize() {
        return DEFAULT_SIZE;
    }

    public static Integer getDEFAULT_PAGE() {
        return DEFAULT_PAGE;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public Boolean getDescending() {
        return descending;
    }

    public void setDescending(Boolean descending) {
        this.descending = descending;
    }
}
