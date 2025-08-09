package com.stocks.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StrikeTO {
	private double curPrice;
	private double strikePrice;
	private int ceOi; // Open Interest for Call Option
	private double ceOiChg;
	private double ceLtpChg; // Change in last traded price
	private String ceOiInt;  // Open Interest in integer format
	private int ceVolume;
	private double ceIv;
	private double ceIvChg; // Change in Implied Volatility
	private int peOi; // Open Interest for Put Option
	private double peOiChg;
	private String peOiInt; // Open Interest in integer format
	private int peVolume;
	private double peIv;
	private double peIvChg; // Change in Implied Volatility
	private double peLtpChg;      // Change in last traded price
	private boolean isHighCeOiChg; // Indicates if Call OI change is high
	private boolean isHighPeOiChg; // Indicates if Put OI change is high
	private boolean isLowPeOiChg;  // Indicates if Put OI change is low
	private boolean isLowCeOiChg;  // Indicates if Call LTP change is high
}
