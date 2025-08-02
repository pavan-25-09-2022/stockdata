package com.stocks.dto;

public class MarketMoverData {

    private String stSymbolName;
    private String stFetchTime;
    private String inOldOi;
    private String inOldClose;
    private String inNewOi;
    private String inNewClose;
    private double inNewDayOpen;
    private double inNewDayHigh;
    private double inNewDayLow;

    public String getStSymbolName() { return stSymbolName; }
    public void setStSymbolName(String stSymbolName) { this.stSymbolName = stSymbolName; }

    public String getStFetchTime() { return stFetchTime; }
    public void setStFetchTime(String stFetchTime) { this.stFetchTime = stFetchTime; }

    public String getInOldOi() { return inOldOi; }
    public void setInOldOi(String inOldOi) { this.inOldOi = inOldOi; }

    public String getInOldClose() { return inOldClose; }
    public void setInOldClose(String inOldClose) { this.inOldClose = inOldClose; }

    public String getInNewOi() { return inNewOi; }
    public void setInNewOi(String inNewOi) { this.inNewOi = inNewOi; }

    public String getInNewClose() { return inNewClose; }
    public void setInNewClose(String inNewClose) { this.inNewClose = inNewClose; }

    public double getInNewDayOpen() { return inNewDayOpen; }
    public void setInNewDayOpen(double inNewDayOpen) { this.inNewDayOpen = inNewDayOpen; }

    public double getInNewDayHigh() { return inNewDayHigh; }
    public void setInNewDayHigh(double inNewDayHigh) { this.inNewDayHigh = inNewDayHigh; }

    public double getInNewDayLow() { return inNewDayLow; }
    public void setInNewDayLow(double inNewDayLow) { this.inNewDayLow = inNewDayLow; }

}
