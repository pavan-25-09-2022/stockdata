package com.stocks.dto;

import java.util.List;

public class StockEODResponse {

    private String status;
    private String msg;
    private List<StockEODDataResponse> data;

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

    public List<StockEODDataResponse> getData() {
        return data;
    }

    public void setData(List<StockEODDataResponse> data) {
        this.data = data;
    }
}
