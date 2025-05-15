package com.stocks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenHighLowResponse {
    public List<Data> getData() {
        return data;
    }

    public void setData(List<Data> data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    private String status;
    private String msg;
    private List<OpenHighLowResponse.Data> data;

    public static class Data {
        @JsonProperty("stSymbolName")
        private String symbol;

        @JsonProperty("inOldLtp")
        private double oldLtp;

        @JsonProperty("inOldDayOpen")
        private double oldOpen;

        @JsonProperty("inOldDayHigh")
        private double oldHigh;

        @JsonProperty("inOldDayLow")
        private double oldLow;

        @JsonProperty("isOH")
        private boolean isHigh;

        @JsonProperty("isOL")
        private boolean isLow;

        @JsonProperty("inNewLtp")
        private double newLtp;

        @JsonProperty("inNewDayHigh")
        private double newHigh;

        @JsonProperty("inNewDayLow")
        private double newLow;

        @JsonProperty("stOldDayHighBreakTime")
        private String oldDayHighBreakTime;

        @JsonProperty("stOldDayLowBreakTime")
        private String oldDayLowBreakTime;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public double getOldLtp() {
            return oldLtp;
        }

        public void setOldLtp(double oldLtp) {
            this.oldLtp = oldLtp;
        }

        public double getOldOpen() {
            return oldOpen;
        }

        public void setOldOpen(double oldOpen) {
            this.oldOpen = oldOpen;
        }

        public double getOldHigh() {
            return oldHigh;
        }

        public void setOldHigh(double oldHigh) {
            this.oldHigh = oldHigh;
        }

        public double getOldLow() {
            return oldLow;
        }

        public void setOldLow(double oldLow) {
            this.oldLow = oldLow;
        }

        public boolean isHigh() {
            return isHigh;
        }

        public void setHigh(boolean high) {
            isHigh = high;
        }

        public boolean isLow() {
            return isLow;
        }

        public void setLow(boolean low) {
            isLow = low;
        }

        public double getNewLtp() {
            return newLtp;
        }

        public void setNewLtp(double newLtp) {
            this.newLtp = newLtp;
        }

        public double getNewHigh() {
            return newHigh;
        }

        public void setNewHigh(double newHigh) {
            this.newHigh = newHigh;
        }

        public double getNewLow() {
            return newLow;
        }

        public void setNewLow(double newLow) {
            this.newLow = newLow;
        }

        public String getOldDayHighBreakTime() {
            return oldDayHighBreakTime;
        }

        public void setOldDayHighBreakTime(String oldDayHighBreakTime) {
            this.oldDayHighBreakTime = oldDayHighBreakTime;
        }

        public String getOldDayLowBreakTime() {
            return oldDayLowBreakTime;
        }

        public void setOldDayLowBreakTime(String oldDayLowBreakTime) {
            this.oldDayLowBreakTime = oldDayLowBreakTime;
        }
    }
}
