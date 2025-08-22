package com.stocks.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Data
@AllArgsConstructor
public class DayLowAndHigh {

    private BigDecimal dayHigh;
    private  BigDecimal dayLow;
    private String stock;
    private boolean isDayHighBreak;
    private boolean isDayLowBreak;
}
