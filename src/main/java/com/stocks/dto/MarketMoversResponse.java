package com.stocks.dto;

import java.util.List;

public class MarketMoversResponse {

    private String status;
    private String msg;
    private List<MarketMoverData> data;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }

    public List<MarketMoverData> getData() { return data; }
    public void setData(List<MarketMoverData> data) { this.data = data; }
}
