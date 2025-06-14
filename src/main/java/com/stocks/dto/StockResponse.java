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
    private Candle validCandle;

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
}
