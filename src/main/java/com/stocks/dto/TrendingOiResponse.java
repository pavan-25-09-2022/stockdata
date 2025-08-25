// RootResponse.java
package com.stocks.dto;

public class TrendingOiResponse {
    private String status;
    private String msg;
    private TrendingOiDataWrapper data;

    // Getters and setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public TrendingOiDataWrapper getData() {
        return data;
    }

    public void setData(TrendingOiDataWrapper data) {
        this.data = data;
    }
}