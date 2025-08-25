package com.stocks.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocks.dto.*;
import com.stocks.dto.Properties;
import com.stocks.utils.FormatUtil;
import com.stocks.utils.MarketHolidayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IOPulseService {

	private static final Logger log = LoggerFactory.getLogger(IOPulseService.class);

	@Autowired
	private RestTemplate restTemplate;

	@Value("${api.url}")
	private String apiUrl;

	@Value("${api.month.url}")
	private String apiMonthUrl;

	@Value("${api.option.chain.url}")
	private String apiOptionChainUrl;

	@Value("${api.trending.oi.url}")
	private String apiTrendingOiUrl;

	@Value("${api.market.movers}")
	private String apiMarketMoversUrl;

	@Value("${api.auth.token}")
	private String authToken;

	@Value(("${exitTime}"))
	private String exitTime;

	@Value("${endTime}")
	private String endTime;

	public Set<String> getAvailableStocks() {
		try {
			String url = "https://api.oipulse.com/api/options/getavailableoptionsdata";
			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", authToken);
			headers.set("Content-Type", "application/json");
			Map<String, String> payload = new HashMap<>();
			payload.put("stSelectedModeOfData", "live");
			HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);


			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
			if (response.getBody() != null) {
				ObjectMapper objectMapper = new ObjectMapper();
				FandOStocksTO apiResponse = objectMapper.readValue(response.getBody(), FandOStocksTO.class);
				// Now use apiResponse as needed
				if (apiResponse != null) {
					Set<String> availableStocks = new HashSet<>();
					for (OptionData data : apiResponse.getData()) {
						if (data.getType().equals("FUTSTK")) {
							availableStocks.add(data.getText());
						}
					}
					return availableStocks;
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return new HashSet<>();
		}
		return new HashSet<>();
	}

	ApiResponse sendRequest(Properties properties, String stock) {
		try {
			// Create payload

			String selectedDate = ((properties.getStockDate() != null && !properties.getStockDate().isEmpty())) ? properties.getStockDate() : LocalDate.now().toString();
			String workingDay = MarketHolidayUtils.getWorkingDay(selectedDate);
			Map<String, String> payload = new HashMap<>();
			payload.put("stSelectedFutures", stock);
			payload.put("stSelectedExpiry", "I");
			payload.put("stSelectedAvailableDate", selectedDate);
			// Determine if the selected date is in the past
			LocalDate selected = LocalDate.parse(selectedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			LocalDate today = LocalDate.now();
			if (selected.isBefore(today)) {
				payload.put("stSelectedModeOfData", "historical");
			} else {
				payload.put("stSelectedModeOfData", "live");
			}

			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", authToken);
			headers.set("Content-Type", "application/json");

			// Create request entity
			HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

			// Make POST request
			ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, requestEntity, String.class);
			if (response.getBody() != null) {
				ObjectMapper objectMapper = new ObjectMapper();
				ApiResponse apiResponse = objectMapper.readValue(response.getBody(), ApiResponse.class);
				// Now use apiResponse as needed
				return apiResponse;
			}

		} catch (Exception e) {
			log.error("Error in sendRequest", e);
		}
		return null;
	}

	StockEODResponse getMonthlyData(String stock) {
		// Create payload
		Map<String, String> payload = new HashMap<>();
		payload.put("stSelectedFutures", stock);
		payload.put("stSelectedExpiry", "I");

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", authToken);
		headers.set("Content-Type", "application/json");

		// Create request entity
		HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

		// Make POST request
		ResponseEntity<StockEODResponse> response = restTemplate.exchange(apiMonthUrl, HttpMethod.POST, requestEntity, StockEODResponse.class);
		return response.getBody();
	}

	public OptionChainResponse getOptionChain(Properties properties, String stock, LocalTime startTime) {
		// Create payload
		Map<String, String> payload = new HashMap<>();

		String startStringTime = startTime != null ? startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "09:15:00";
		String endStringTime = properties.getEndTime() != null ? properties.getEndTime() : FormatUtil.formatTimeHHmmss(FormatUtil.getTime(startStringTime, properties.getInterval()));
		properties.setStartTime(startStringTime);
		properties.setEndTime(endStringTime);
		payload.put("stSelectedOptions", stock);
		String selectedDate = ((properties.getStockDate() != null && !properties.getStockDate().isEmpty())) ? properties.getStockDate() : LocalDate.now().toString();

		payload.put("stSelectedAvailableDate", selectedDate);
		payload.put("stSelectedAvailableExpiryDate", properties.getExpiryDate());
		LocalDate selected = LocalDate.parse(selectedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		LocalDate today = LocalDate.now();
		if (selected.isBefore(today)) {
			payload.put("stSelectedModeOfData", "historical");
		} else {
			payload.put("stSelectedModeOfData", "live");
		}
		payload.put("inSelectedavailableTimeRange", "CUSTOM_TIME");
		payload.put("stStartTime", startStringTime);
		payload.put("stEndTime", endStringTime);


		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", authToken);
		headers.set("Content-Type", "application/json");

		// Create request entity
		HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

		// Make POST request
		try {
			ResponseEntity<OptionChainResponse> response = restTemplate.exchange(apiOptionChainUrl, HttpMethod.POST, requestEntity, OptionChainResponse.class);
			log.info("stock {} startTime {} endTime {}", stock, properties.getStartTime(), properties.getEndTime());
			return response.getBody();
		} catch (Exception e) {
			//log.error("Error in getOptionChain", e);
			return null;
		}
	}

	public MarketMoversResponse marketMovers(Properties properties) {
		try {
			// Create payload

			String selectedDate = ((properties.getStockDate() != null && !properties.getStockDate().isEmpty())) ? properties.getStockDate() : LocalDate.now().toString();
			Map<String, String> payload = new HashMap<>();
			payload.put("stSelectedExpiry", "I");
			payload.put("stSelectedAvailableDate", selectedDate);
			// Determine if the selected date is in the past
			LocalDate selected = LocalDate.parse(selectedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			LocalDate today = LocalDate.now();
			if (selected.isBefore(today)) {
				payload.put("stSelectedModeOfData", "historical");
			} else {
				payload.put("stSelectedModeOfData", "live");
			}

			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", authToken);
			headers.set("Content-Type", "application/json");

			// Create request entity
			HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

			// Make POST request
			ResponseEntity<MarketMoversResponse> response = restTemplate.exchange(apiMarketMoversUrl, HttpMethod.POST, requestEntity, MarketMoversResponse.class);
			return response.getBody();
		} catch (Exception e) {
			log.error("Error in market movers ", e);
			return null;
		}
	}

	public CandleDataResponse getCandleData(Properties properties, Long fromTs, Long toTs) {
		try {
			String url = "https://api.oipulse.com/api/trading-view/getcandledata";
			// Create payload
			Map<String, String> payload = new HashMap<>();
			payload.put("ex", "NSE");
			payload.put("symbol", properties.getStockName());
			payload.put("limit", "1500");
			payload.put("type", "stocks");
			payload.put("countBack", "50");
			payload.put("resolution", properties.getInterval() + "");
			payload.put("fromTs", fromTs.toString());
			payload.put("toTs", toTs.toString());

			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", authToken);
			headers.set("Content-Type", "application/json");

			// Create request entity
			HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(payload, headers);

			// Make POST request
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
			if (response.getBody() != null) {
				ObjectMapper objectMapper = new ObjectMapper();
				CandleDataResponse apiResponse = objectMapper.readValue(response.getBody(), CandleDataResponse.class);
				// Now use apiResponse as needed
				return apiResponse;
			}

		} catch (Exception e) {
			log.error("Error in stock Request", e);
		}
		return null;
	}

	private List<Double> getStrikes(Properties properties, String stock, String time) {
		LocalTime startTime = FormatUtil.getTime(time, 0);
		OptionChainResponse response = getOptionChain(properties, stock, startTime);
		if (response == null || response.getData() == null) {
			return null;
		}

		UnderLyingAssetData underLyingAssetData = response.getData().getUnderLyingAssetData();
		List<OptionChainData> list = response.getData().getData();

		TreeMap<Double, List<OptionChainData>> groupedData = list.stream()
				.collect(Collectors.groupingBy(
						OptionChainData::getInStrikePrice,
						TreeMap::new,
						Collectors.toList()
				));

		Double focusKey = groupedData.floorKey((double) underLyingAssetData.getInLtp());
		if (focusKey == null) {
			return null;
		}

		List<Double> keys = new ArrayList<>(groupedData.keySet());
		int focusIndex = keys.indexOf(focusKey);
		if (focusIndex == -1) {
			return null;
		}

		int from = Math.max(0, focusIndex - properties.getTrendOiStrikes());
		int to = Math.min(keys.size(), focusIndex + properties.getTrendOiStrikes() + 1);
		return keys.subList(from, to);
	}

	public TrendingOiResponse getTrendingOI(Properties properties) {
		try {
			// Create payload
			Map<String, Object> payload = new HashMap<>();
			properties.setStartTime("09:15:00");
			properties.setEndTime("15:30:00");
			List<Double> strikes = getStrikes(properties, properties.getStockName(), properties.getStartTime());
			System.out.println("Strike prices for " + properties.getStockName() + ": " + strikes);
			payload.put("selectedStrikePrices", strikes);
			payload.put("stSelectedAsset", properties.getStockName());
			String selectedDate = ((properties.getStockDate() != null && !properties.getStockDate().isEmpty())) ? properties.getStockDate() : LocalDate.now().toString();
			payload.put("stSelectedAvailableDate", selectedDate);
			payload.put("stSelectedAvailableExpiryDate", properties.getExpiryDate());
			LocalDate selected = LocalDate.parse(selectedDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			LocalDate today = LocalDate.now();
			if (selected.isBefore(today)) {
				payload.put("stSelectedModeOfData", "historical");
			} else {
				payload.put("stSelectedModeOfData", "live");
			}

			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", authToken);
			headers.set("Content-Type", "application/json");

			// Create request entity
			HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

			// Make POST request
			ResponseEntity<TrendingOiResponse> response = restTemplate.exchange(apiTrendingOiUrl, HttpMethod.POST, requestEntity, TrendingOiResponse.class);
			return response.getBody();
		} catch (Exception e) {
			log.error("Error in trending OI ", e);
			return null;
		}
	}


}
