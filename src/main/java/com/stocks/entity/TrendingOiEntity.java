package com.stocks.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
public class TrendingOiEntity {
    private String stockSymbol;
    private String stockDate;
    private String fetchTime;
    private Double changeInCallOi;
    private Double changeInPutOi;
    private Double diffInOi;
    private Double changeInDirectionOi;
    private Double directionOfChangePercentage;
    private Double netPcr;
    private String sentiment;
    private boolean isPEShorted;
    private boolean isCEShorted;
    private Double close;
    private Double high;
    private Double low;
    private Double open;

}
