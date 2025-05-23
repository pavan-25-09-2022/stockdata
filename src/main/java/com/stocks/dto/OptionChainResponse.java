package com.stocks.dto;

public class OptionChainResponse {
    private String status;
    private String msg;
    private OptionChainDataWrapper data;

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

    public OptionChainDataWrapper getData() {
        return data;
    }

    public void setData(OptionChainDataWrapper data) {
        this.data = data;
    }
}