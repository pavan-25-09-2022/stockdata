package com.stocks.dto;

import lombok.Data;

@Data
public class EodAnalyzerTO {
    private String stockSymbol;
    private String stockDate;
    private String oiInterpretation;
}
