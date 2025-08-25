package com.stocks.utils;

import com.stocks.dto.StrikeTO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CommonUtils {

	public StrikeTO getLargestCeVolumeStrike(Map<Integer, StrikeTO> strikes) {
		if (strikes == null || strikes.isEmpty()) return null;
		StrikeTO maxStrike = null;
		int maxCeVolume = Integer.MIN_VALUE;
		for (StrikeTO strike : strikes.values()) {
			if (strike != null && strike.getCeVolume() > maxCeVolume) {
				maxCeVolume = strike.getCeVolume();
				maxStrike = strike;
			}
		}
		return maxStrike;
	}

	public StrikeTO getLargestPeVolumeStrike(Map<Integer, StrikeTO> strikes) {
		if (strikes == null || strikes.isEmpty()) return null;
		StrikeTO maxStrike = null;
		int maxPeVolume = Integer.MIN_VALUE;
		for (StrikeTO strike : strikes.values()) {
			if (strike != null && strike.getPeVolume() > maxPeVolume) {
				maxPeVolume = strike.getPeVolume();
				maxStrike = strike;
			}
		}
		return maxStrike;
	}

	public StrikeTO getTarget2Strike(Map<Integer, StrikeTO> strikes) {
		StrikeTO strike = null;
		if (strikes.get(1) != null && strikes.get(1).getPeOiChg() < 0) {
			strike = strikes.get(0) != strike ? strikes.get(0) : null;
		} else if (strikes.get(2) != null && strikes.get(2).getPeOiChg() < 0) {
			strike = strikes.get(1) != null ? strikes.get(1) : null;
		} else if (strikes.get(3) != null && strikes.get(3).getPeOiChg() < 0) {
			strike = strikes.get(2) != null ? strikes.get(2) : null;
		} else if (strikes.get(4) != null && strikes.get(4).getPeOiChg() < 0) {
			strike = strikes.get(3) != null ? strikes.get(3) : null;
		} else {
			strike = strikes.get(4) != null ? strikes.get(4) : null;
		}
		return strike;
	}
}
