// ResponseData.java
package com.stocks.dto;

import java.util.List;

public class TrendingOiDataWrapper {
    private List<TrendingOiData> data;

    // Getters and setters
    public List<TrendingOiData> getData() {
        return data;
    }

    public void setData(List<TrendingOiData> data) {
        this.data = data;
    }
}