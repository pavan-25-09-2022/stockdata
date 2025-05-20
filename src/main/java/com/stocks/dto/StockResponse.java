package com.stocks.dto;

import com.stocks.utils.FormatUtil;
import lombok.Data;

import java.time.LocalTime;

@Data
public class StockResponse {

    private String stock;
    private LocalTime startTime;
    private LocalTime endTime;
    private String oiInterpretation;
    private double stopLoss;
    private double limit;
    private double currentPrice;
    private String volume;
    String stockType = "";
    String eodData;
    StockProfitResult stockProfitResult;
    int priority;
    private double curHigh;
    private double curLow;
    private String yestDayBreak = "N";
    private String optionChain = "N";
    private String trend ="";

    private OptionChainData row5put;
    private OptionChainData row5call;

    private OptionChainData curput;
    private OptionChainData curcall;

    private OptionChainData row6put;
    private OptionChainData row6call;
    private String rsi;
    private double chgeInPer;
    private String curSt;
    private String firstChgeInPer;
    private String putSt;
    private String callSt;
    private String cay;
    private Candle firstCandle;
    private Candle curCandle;

    public StockResponse(){

    }

    public StockResponse(String stock, String stockType, String startTime, String time, String oiInterpretation, double stopLoss, double currentPrice, long volume) {
        this.stock = stock;
        this.startTime = FormatUtil.getTime(startTime,-1);
        this.endTime = FormatUtil.getTime(time,0);
        this.oiInterpretation = oiInterpretation;
        this.stopLoss = stopLoss;
        this.currentPrice = currentPrice;
        this.stockType = stockType;
        this.volume = FormatUtil.formatVolume(volume);
    }

    public String getStock() {
        return stock;
    }

    public void setStock(String stock) {
        this.stock = stock;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getOiInterpretation() {
        return oiInterpretation;
    }

    public void setOiInterpretation(String oiInterpretation) {
        this.oiInterpretation = oiInterpretation;
    }

    public double getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(double stopLoss) {
        this.stopLoss = stopLoss;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getStockType() {
        return stockType;
    }

    public void setStockType(String stockType) {
        this.stockType = stockType;
    }

    public StockProfitResult getStockProfitResult() {
        return stockProfitResult;
    }

    public void setStockProfitResult(StockProfitResult stockProfitResult) {
        this.stockProfitResult = stockProfitResult;
    }

    public String getEodData() {
        return eodData;
    }

    public void setEodData(String eodData) {
        this.eodData = eodData;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public String getVolume() {
        return volume;
    }

    public String setVolume(String volume) {
        this.volume = volume;
        return volume;
    }

    public double getCurHigh() {
        return curHigh;
    }

    public void setCurHigh(double curHigh) {
        this.curHigh = curHigh;
    }

    public double getCurLow() {
        return curLow;
    }

    public void setCurLow(double curLow) {
        this.curLow = curLow;
    }

    public String getYestDayBreak() {
        return yestDayBreak;
    }

    public void setYestDayBreak(String yestDayBreak) {
        this.yestDayBreak = yestDayBreak;
    }

    public OptionChainData getRow5put() {
        return row5put;
    }

    public void setRow5put(OptionChainData row5put) {
        this.row5put = row5put;
    }

    public OptionChainData getRow5call() {
        return row5call;
    }

    public void setRow5call(OptionChainData row5call) {
        this.row5call = row5call;
    }

    public OptionChainData getCurput() {
        return curput;
    }

    public void setCurput(OptionChainData curput) {
        this.curput = curput;
    }

    public OptionChainData getCurcall() {
        return curcall;
    }

    public void setCurcall(OptionChainData curcall) {
        this.curcall = curcall;
    }

    public OptionChainData getRow6put() {
        return row6put;
    }

    public void setRow6put(OptionChainData row6put) {
        this.row6put = row6put;
    }

    public OptionChainData getRow6call() {
        return row6call;
    }

    public void setRow6call(OptionChainData row6call) {
        this.row6call = row6call;
    }

    public String getOptionChain() {
        return optionChain;
    }

    public void setOptionChain(String optionChain) {
        this.optionChain = optionChain;
    }
    public String getRsi() {
        return rsi;
    }
    public void setRsi(double rsi) {
        this.rsi = String.format("%.2f", rsi);
    }
    public double getChgeInPer() {
        return chgeInPer;
    }
    public void setChgeInPer(double chgeInPer) {
        this.chgeInPer = chgeInPer;
    }

    public String getCurSt() {
        return curSt;
    }
    public void setCurSt(String curSt) {
        this.curSt = curSt;
    }
    public String getPutSt() {
        return putSt;
    }
    public void setPutSt(String putSt) {
        this.putSt = putSt;
    }
    public String getCallSt() {
        return callSt;
    }
    public void setCallSt(String callSt) {
        this.callSt = callSt;
    }

    public String getFirstChgeInPer() {
        return firstChgeInPer;
    }

    public void setFirstChgeInPer(double firstChgeInPer) {
        this.firstChgeInPer = String.format("%.2f", firstChgeInPer);
    }

    public String getCay() {
        return cay;
    }
    public void setCay(String cay) {
        this.cay = cay;
    }
    public double getLimit() {
        return limit;
    }
    public void setLimit(double limit) {
        this.limit = limit;
    }
    public String getTrend() {
        return trend;
    }
    public void setTrend(String trend) {
        this.trend = trend;
    }
}
