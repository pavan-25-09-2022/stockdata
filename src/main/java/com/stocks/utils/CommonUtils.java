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

    public StrikeTO getHighestCeOiChangeStrike(Map<Integer, StrikeTO> strikes) {
        if (strikes == null || strikes.isEmpty()) return null;
        StrikeTO maxStrike = null;
        double maxCeOiChange = Double.NEGATIVE_INFINITY;
        for (StrikeTO strike : strikes.values()) {
            if (strike != null && strike.getCeOiChg() > maxCeOiChange) {
                maxCeOiChange = strike.getCeOiChg();
                maxStrike = strike;
            }
        }
        return maxStrike;
    }

    public StrikeTO getLowestCeOiChangeStrike(Map<Integer, StrikeTO> strikes) {
        if (strikes == null || strikes.isEmpty()) return null;
        StrikeTO minStrike = null;
        double minCeOiChange = Double.POSITIVE_INFINITY;
        for (StrikeTO strike : strikes.values()) {
            if (strike != null && strike.getCeOiChg() < minCeOiChange) {
                minCeOiChange = strike.getCeOiChg();
                minStrike = strike;
            }
        }
        return minStrike;
    }

    public StrikeTO getHighestPeOiChangeStrike(Map<Integer, StrikeTO> strikes) {
        if (strikes == null || strikes.isEmpty()) return null;
        StrikeTO maxStrike = null;
        double maxPeOiChange = Double.NEGATIVE_INFINITY;
        for (StrikeTO strike : strikes.values()) {
            if (strike != null && strike.getPeOiChg() > maxPeOiChange) {
                maxPeOiChange = strike.getPeOiChg();
                maxStrike = strike;
            }
        }
        return maxStrike;
    }

    public StrikeTO getLowestPeOiChangeStrike(Map<Integer, StrikeTO> strikes) {
        if (strikes == null || strikes.isEmpty()) return null;
        StrikeTO minStrike = null;
        double minPeOiChange = Double.POSITIVE_INFINITY;
        for (StrikeTO strike : strikes.values()) {
            if (strike != null && strike.getPeOiChg() < minPeOiChange) {
                minPeOiChange = strike.getPeOiChg();
                minStrike = strike;
            }
        }
        return minStrike;
    }
}
