package com.stocks.yahoo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocks.enumaration.QueryInterval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yahoofinance.Utils;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.query2v8.HistQuotesQuery2V8Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HistQuotesQuery2V8RequestImpl extends HistQuotesQuery2V8Request {

	private static final Logger log = LoggerFactory.getLogger(HistQuotesQuery2V8RequestImpl.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private final String symbol;
	private final Calendar fromTime;
	private final Calendar toTime;
	private final QueryInterval interval;

	public HistQuotesQuery2V8RequestImpl(String symbol, Calendar from, Calendar calendar, QueryInterval interval) {
		super(symbol);
		this.symbol = symbol;
		this.fromTime = from;
		toTime = calendar;
		this.interval = interval;
	}

	@Override
	public List<HistoricalQuote> getResult() throws IOException {
		String json = this.getJson();
		JsonNode resultNode = objectMapper.readTree(json).get("chart").get("result").get(0);
		JsonNode dayHigh = resultNode.get("regularMarketDayHigh");
		JsonNode dayLow = resultNode.get("regularMarketDayLow");
		JsonNode timestamps = resultNode.get("timestamp");
		JsonNode indicators = resultNode.get("indicators");
		JsonNode quotes = indicators.get("quote").get(0);
		JsonNode closes = quotes.get("close");
		JsonNode volumes = quotes.get("volume");
		JsonNode opens = quotes.get("open");
		JsonNode highs = quotes.get("high");
		JsonNode lows = quotes.get("low");
		//JsonNode adjCloses = indicators.get("adjclose").get(0).get("adjclose");
		List<HistoricalQuote> result = new ArrayList();

		for (int i = 0; i < timestamps.size(); ++i) {
			long timestamp = timestamps.get(i).asLong();
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(timestamp * 1000L);
			// BigDecimal adjClose = adjCloses.get(i).decimalValue();
			long volume = volumes.get(i).asLong();
			BigDecimal open = opens.get(i).decimalValue();
			BigDecimal high = highs.get(i).decimalValue();
			BigDecimal low = lows.get(i).decimalValue();
			BigDecimal close = closes.get(i).decimalValue();
			HistoricalQuote quote = new HistoricalQuote(this.symbol, calendar, open, low, high, close, null, volume);
			result.add(quote);
		}

		return result;
	}

	public List<com.stocks.dto.HistoricalQuote> getCompleteResult() throws IOException {
		String json = this.getJson();
		JsonNode resultNode = objectMapper.readTree(json).get("chart").get("result").get(0);
		JsonNode dayHigh = resultNode.get("meta").get("regularMarketDayHigh");
		JsonNode dayLow = resultNode.get("meta").get("regularMarketDayLow");
		JsonNode timestamps = resultNode.get("timestamp");
		JsonNode indicators = resultNode.get("indicators");
		JsonNode quotes = indicators.get("quote").get(0);
		JsonNode closes = quotes.get("close");
		JsonNode volumes = quotes.get("volume");
		JsonNode opens = quotes.get("open");
		JsonNode highs = quotes.get("high");
		JsonNode lows = quotes.get("low");
		List<com.stocks.dto.HistoricalQuote> result = new ArrayList();

		if (timestamps == null || timestamps.size() == 0) {
			log.warn("No historical data found for symbol: " + this.symbol);
			return result;
		}

		for (int i = 0; i < timestamps.size(); ++i) {
			long timestamp = timestamps.get(i).asLong();
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(timestamp * 1000L);
			long volume = volumes.get(i).asLong();
			BigDecimal open = opens.get(i).decimalValue();
			BigDecimal high = highs.get(i).decimalValue();
			BigDecimal low = lows.get(i).decimalValue();
			BigDecimal close = closes.get(i).decimalValue();
			com.stocks.dto.HistoricalQuote quote = new com.stocks.dto.HistoricalQuote(this.symbol, calendar, open, low, high, close, null, volume, dayHigh.decimalValue(), dayLow.decimalValue());
			result.add(quote);
		}

		return result;
	}

	@Override
	public String getJson() throws IOException {
		if (this.fromTime.after(this.toTime)) {
			log.warn("Unable to retrieve historical quotes. From-date should not be after to-date. From: " + this.fromTime.getTime() + ", to: " + this.toTime.getTime());
			return "";
		} else {
			Map<String, String> params = new LinkedHashMap();
			params.put("period1", String.valueOf(this.fromTime.getTimeInMillis() / 1000L));
			params.put("period2", String.valueOf(this.toTime.getTimeInMillis() / 1000L));
			//params.put("metrics", "high");
			params.put("interval", interval.getTag());
			// params.put("range", "15m");
			String url = YahooFinance.HISTQUOTES_QUERY2V8_BASE_URL + URLEncoder.encode(this.symbol, "UTF-8") + "?" + Utils.getURLParameters(params);
			log.info("Sending request: " + url);
			URL request = new URL(url);
			// RedirectableRequest redirectableRequest = new RedirectableRequest(request, 5);
			//redirectableRequest.setConnectTimeout(YahooFinance.CONNECTION_TIMEOUT);
			//redirectableRequest.setReadTimeout(YahooFinance.CONNECTION_TIMEOUT);
			URLConnection connection = request.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
			InputStreamReader is = new InputStreamReader(connection.getInputStream());
			BufferedReader br = new BufferedReader(is);
			StringBuilder builder = new StringBuilder();

			for (String line = br.readLine(); line != null; line = br.readLine()) {
				if (builder.length() > 0) {
					builder.append("\n");
				}
				builder.append(line);
			}
			return builder.toString();
		}
	}
}
