package com.stocks.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Calendar;


@Data
@NoArgsConstructor
public class HistoricalQuote {


	private String symbol;
	private Calendar date;
	private BigDecimal open;
	private BigDecimal low;
	private BigDecimal high;
	private BigDecimal close;
	private BigDecimal adjClose;
	private Long volume;
	private BigDecimal dayHigh;
	private BigDecimal dayLow;

	public HistoricalQuote(String symbol, Calendar date, BigDecimal open, BigDecimal low, BigDecimal high, BigDecimal close, BigDecimal adjClose, Long volume, BigDecimal dayHigh, BigDecimal dayLow) {
		this.symbol = symbol;
		this.date = date;
		this.open = open;
		this.low = low;
		this.high = high;
		this.close = close;
		this.adjClose = adjClose;
		this.volume = volume;
		this.dayHigh = dayHigh;
		this.dayLow = dayLow;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public Calendar getDate() {
		return date;
	}

	public void setDate(Calendar date) {
		this.date = date;
	}

	public BigDecimal getOpen() {
		return open;
	}

	public void setOpen(BigDecimal open) {
		this.open = open;
	}

	public BigDecimal getLow() {
		return low;
	}

	public void setLow(BigDecimal low) {
		this.low = low;
	}

	public BigDecimal getHigh() {
		return high;
	}

	public void setHigh(BigDecimal high) {
		this.high = high;
	}

	public BigDecimal getClose() {
		return close;
	}

	public void setClose(BigDecimal close) {
		this.close = close;
	}

	public BigDecimal getAdjClose() {
		return adjClose;
	}

	public void setAdjClose(BigDecimal adjClose) {
		this.adjClose = adjClose;
	}

	public Long getVolume() {
		return volume;
	}

	public void setVolume(Long volume) {
		this.volume = volume;
	}

	public BigDecimal getDayHigh() {
		return dayHigh;
	}

	public void setDayHigh(BigDecimal dayHigh) {
		this.dayHigh = dayHigh;
	}

	public BigDecimal getDayLow() {
		return dayLow;
	}

	public void setDayLow(BigDecimal dayLow) {
		this.dayLow = dayLow;
	}

	@Override
	public String toString() {
		return "HistoricalQuote{" +
				"symbol='" + symbol + '\'' +
				", date=" + date +
				", open=" + open +
				", low=" + low +
				", high=" + high +
				", close=" + close +
				", adjClose=" + adjClose +
				", volume=" + volume +
				", dayHigh=" + dayHigh +
				", dayLow=" + dayLow +
				'}';
	}
}

