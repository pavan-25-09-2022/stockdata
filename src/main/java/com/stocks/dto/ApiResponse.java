package com.stocks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ApiResponse {

    private String status;
    private String msg;
    private List<Data> data;

    // Getters and Setters
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

    public List<Data> getData() {
        return data;
    }

    public void setData(List<Data> data) {
        this.data = data;
    }

    public static class Data {
        @JsonProperty("stDate")
        private String date;

        @JsonProperty("stTime")
        private String time;

        @JsonProperty("stDataFetchType")
        private String dataFetchType;

        @JsonProperty("inOi")
        private String openInterest;

        @JsonProperty("inOpen")
        private double open;

        @JsonProperty("inHigh")
        private double high;

        @JsonProperty("inLow")
        private double low;

        @JsonProperty("inClose")
        private double close;

        @JsonProperty("inDayOpen")
        private double dayOpen;

        @JsonProperty("inDayHigh")
        private double dayHigh;

        @JsonProperty("inDayLow")
        private double dayLow;

        @JsonProperty("inTradedVolume")
        private long tradedVolume;

        // Getters and Setters
        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getDataFetchType() {
            return dataFetchType;
        }

        public void setDataFetchType(String dataFetchType) {
            this.dataFetchType = dataFetchType;
        }

        public String getOpenInterest() {
            return openInterest;
        }

        public void setOpenInterest(String openInterest) {
            this.openInterest = openInterest;
        }

        public double getOpen() {
            return open;
        }

        public void setOpen(double open) {
            this.open = open;
        }

        public double getHigh() {
            return high;
        }

        public void setHigh(double high) {
            this.high = high;
        }

        public double getLow() {
            return low;
        }

        public void setLow(double low) {
            this.low = low;
        }

        public double getClose() {
            return close;
        }

        public void setClose(double close) {
            this.close = close;
        }

        public double getDayOpen() {
            return dayOpen;
        }

        public void setDayOpen(double dayOpen) {
            this.dayOpen = dayOpen;
        }

        public double getDayHigh() {
            return dayHigh;
        }

        public void setDayHigh(double dayHigh) {
            this.dayHigh = dayHigh;
        }

        public double getDayLow() {
            return dayLow;
        }

        public void setDayLow(double dayLow) {
            this.dayLow = dayLow;
        }

        public long getTradedVolume() {
            return tradedVolume;
        }

        public void setTradedVolume(long tradedVolume) {
            this.tradedVolume = tradedVolume;
        }
    }
}