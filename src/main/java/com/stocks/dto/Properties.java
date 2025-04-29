package com.stocks.dto;

public class Properties {
    private String stockDate;
    private int exitMins;
    private int interval;
    private boolean fetchAll;
    private int amtInvested;

    public String getStockDate() {
        return stockDate;
    }

    public void setStockDate(String stockDate) {
        this.stockDate = stockDate;
    }

    public int getExitMins() {
        return exitMins;
    }

    public void setExitMins(int exitMins) {
        this.exitMins = exitMins;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public boolean isFetchAll() {
        return fetchAll;
    }

    public void setFetchAll(boolean fetchAll) {
        this.fetchAll = fetchAll;
    }

    public int getAmtInvested() {
        return amtInvested;
    }

    public void setAmtInvested(int amtInvested) {
        this.amtInvested = amtInvested;
    }
}
