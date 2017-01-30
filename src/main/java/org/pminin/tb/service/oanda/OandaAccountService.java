package org.pminin.tb.service.oanda;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.pminin.tb.constants.Constants;
import org.pminin.tb.constants.Step;
import org.pminin.tb.model.AccountDetails;
import org.pminin.tb.model.Candle;
import org.pminin.tb.model.Candle.Candles;
import org.pminin.tb.model.Instrument;
import org.pminin.tb.model.Instrument.Instruments;
import org.pminin.tb.model.Order;
import org.pminin.tb.model.Order.Orders;
import org.pminin.tb.model.Pivot;
import org.pminin.tb.model.PostOrderResponse;
import org.pminin.tb.model.Trade;
import org.pminin.tb.model.Trade.Trades;
import org.pminin.tb.service.AccountService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.social.support.FormMapHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@Service("oandaAccountService")
@Scope("singleton")
public class OandaAccountService implements AccountService {

	private static final String ACCOUNT_DETAILS_API = "v1/accounts/%s";

	private static final String INSTRUMENTS_API = "v1/instruments?accountId=%s&instruments=%s_%s";
	private static final String INSTRUMENTS_API_1 = "v1/instruments?accountId=%s&instruments=%s";
	private static final String CANDLES_API = "v1/candles?accountId=%s&granularity=%s&instrument=%s&start=%d&end=%d&includeFirst=%b";
	private static final String CANDLES_API_COUNT = "v1/candles?accountId=%s&granularity=%s&instrument=%s&count=%d";

	@Autowired
	Logger logger;

	private Config cfg = ConfigFactory.load().getConfig("account.oandaAccountService");

	public OandaAccountService() {
		DateTimeZone.setDefault(DateTimeZone.forID(cfg.getString("timeZone")));
	}

	private String accountId() {
		return cfg.getString("accountId");
	}

	private String accountUrl() {
		return apiUrl() + String.format(ACCOUNT_DETAILS_API, accountId());
	}

	private String apiUrl() {
		return cfg.getString("url");
	}

	private String candlesUrl(Step step, DateTime start, DateTime end, Instrument instrument, boolean includeFirst) {
		return apiUrl() + String.format(CANDLES_API, accountId(), step.toString(), instrument.getInstrument(),
				start.toDate().getTime() / 1000, end.toDate().getTime() / 1000, includeFirst);
	}

	private String candlesUrl(Step step, int count, Instrument instrument) {
		return apiUrl()
				+ String.format(CANDLES_API_COUNT, accountId(), step.toString(), instrument.getInstrument(), count);
	}

	@Override
	public void closeOrdersAndTrades(Instrument instrument) {
		Orders orders = getOrders(instrument);
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + cfg.getString("token"));
		headers.set("X-Accept-Datetime-Format", "UNIX");
		headers.set("X-HTTP-Method-Override", "DELETE");
		HttpEntity<Object> entity = new HttpEntity<>(headers);

		orders.getOrders().stream().forEach(order -> {
			getResponse(updateOrderUrl(order), HttpMethod.POST, entity, Order.class);
		});
		Trades trades = getTrades(instrument);
		trades.getTrades().stream().forEach(trade -> {
			getResponse(updateTradeUrl(trade), HttpMethod.POST, entity, Trade.class);
		});
	}

	@Override
	public Order createOrder(Order order) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + cfg.getString("token"));
		headers.set("X-Accept-Datetime-Format", "UNIX");
		headers.set("Content-Type", "application/x-www-form-urlencoded");
		Map<String, String> map = new HashMap<String, String>();
		map.put("instrument", order.getInstrument().toString());
		map.put("units", String.valueOf(order.getUnits()));
		map.put("side", order.getSide());
		map.put("type", Constants.TYPE_LIMIT);
		map.put("price", String.format("%.5f", order.getPrice()));
		map.put("expiry", String.valueOf(DateTime.now().plusDays(2).getMillis()));
		map.put("stopLoss", String.format("%.5f", order.getStopLoss()));
		map.put("takeProfit", String.format("%.5f", order.getTakeProfit()));
		HttpEntity<Map<String, String>> entity = new HttpEntity<>(map, headers);
		Optional<PostOrderResponse> response = getResponse(ordersUrl(), HttpMethod.POST, entity,
				PostOrderResponse.class);
		return response.orElse(new PostOrderResponse()).getOrderOpened();
	}

	@Override
	public AccountDetails getAccountDetails() {
		Optional<AccountDetails> response = getResponse(accountUrl(), HttpMethod.GET, headers(), AccountDetails.class);
		return response.orElse(new AccountDetails());
	}

	private Candles getCandles(Step step, DateTime start, DateTime end, Instrument instrument, boolean includeFirst) {
		String candlesUrl = candlesUrl(step, start, end, instrument, includeFirst);
		Optional<Candles> candles = getResponse(candlesUrl, HttpMethod.GET, headers(), Candles.class);
		return candles.orElse(new Candles(instrument.toString(), step, new ArrayList<Candle>()));
	}

	@Override
	public Candles getCandles(Step step, DateTime start, Instrument instrument) {
		DateTime end = DateTime.now(DateTimeZone.getDefault());
		return getCandles(step, start, end, instrument, false);
	}

	private Candles getCandles(Step step, int count, Instrument instrument) {
		String candlesUrl = candlesUrl(step, count, instrument);
		Optional<Candles> response = getResponse(candlesUrl, HttpMethod.GET, headers(), Candles.class);
		return response.orElse(new Candles());
	}

	@Override
	public Instrument getInstrument(String left, String right) {
		Optional<Instruments> response = getResponse(instrumentsUrl(left, right), HttpMethod.GET, headers(),
				Instruments.class);
		if (response.isPresent()) {
			List<Instrument> instruments = response.get().getInstruments();
			return instruments.stream().findFirst().orElse(null);
		} else {
			return null;
		}
	}

	@Override
	public Instrument getInstrument(String pair) {
		Optional<Instruments> response = getResponse(instrumentsUrl(pair), HttpMethod.GET, headers(),
				Instruments.class);
		if (response.isPresent()) {
			List<Instrument> instruments = response.get().getInstruments();
			return instruments.stream().findFirst().orElse(null);
		} else {
			return null;
		}
	}

	@Override
	public Orders getOrders(Instrument instrument) {
		Optional<Orders> response = getResponse(getOrdersUrl(instrument), HttpMethod.GET, headers(), Orders.class);
		return response.orElse(new Orders());
	}

	private String getOrdersUrl(Instrument instrument) {
		return String.format(ordersUrl() + "?instrument=%s", instrument.toString());
	}

	@Override
	public Pivot getPivot(Instrument instrument) {
		int count = 1;
		Step step = Step.D;
		Candles candles = getCandles(step, count, instrument);
		try {
			Candle pivotCandle = candles.getCandles().iterator().next();
			if (pivotCandle != null) {
				double high = (pivotCandle.getHighAsk() + pivotCandle.getHighBid()) / 2;
				double low = (pivotCandle.getLowAsk() + pivotCandle.getLowBid()) / 2;
				double close = (pivotCandle.getCloseAsk() + pivotCandle.getCloseBid()) / 2;
				double pp = (high + close + low) / 3;
				double r1 = 2 * pp - low;
				double s1 = 2 * pp - high;
				double r2 = pp + (r1 - s1);
				double s2 = pp - (r1 - s1);
				double r3 = high + 2 * (pp - low);
				double s3 = low - 2 * (high - pp);
				double m5 = (r2 + r3) / 2;
				double m4 = (r1 + r2) / 2;
				double m3 = (pp + r1) / 2;
				double m2 = (pp + s1) / 2;
				double m1 = (s1 + s2) / 2;
				double m0 = (s2 + s3) / 2;
				return new Pivot(pivotCandle.getTime(), instrument, r3, r2, r1, pp, s1, s2, s3, m0, m1, m2, m3, m4, m5);
			}
		} catch (Exception e) {
			logger.error("Could not get pivot points", e);
		}
		return null;
	}

	private <T> Optional<T> getResponse(String url, HttpMethod method, HttpEntity<?> entity,
			Class<T> responseType) {
		T response = null;
		try {
			RestTemplate tmpl = new RestTemplate();
			if (HttpMethod.POST.equals(method) || HttpMethod.PATCH.equals(method)) {
				tmpl.getMessageConverters().add(new FormMapHttpMessageConverter());
				response = tmpl.postForObject(url, entity, responseType);
			} else {
				ResponseEntity<T> resp = tmpl.exchange(url, method, entity, responseType);
				if (resp.getStatusCode() == HttpStatus.OK && resp.hasBody()) {
					response = resp.getBody();
				}
			}
		} catch (HttpStatusCodeException e) {
			logger.error("Could not get response from " + url);
			logger.error(e.getResponseBodyAsString());
		} catch (RestClientException e) {
			logger.error("Could not get response from " + url, e);
		}
		Optional<T> result = Optional.ofNullable(response);
		return result;
	}
	

	@Override
	public Trades getTrades(Instrument instrument) {

		Optional<Trades> response = getResponse(getTradesUrl(instrument), HttpMethod.GET, headers(), Trades.class);
		return response.orElse(new Trades());
	}

	private String getTradesUrl(Instrument instrument) {
		return String.format(tradesUrl() + "?instrument=%s", instrument.toString());
	}

	private HttpEntity<Object> headers() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + cfg.getString("token"));
		headers.set("X-Accept-Datetime-Format", "UNIX");
		HttpEntity<Object> entity = new HttpEntity<>(headers);
		return entity;
	}

	private String instrumentsUrl(String left, String right) {
		return apiUrl() + String.format(INSTRUMENTS_API, accountId(), left, right);
	}

	private String instrumentsUrl(String pair) {
		return apiUrl() + String.format(INSTRUMENTS_API_1, accountId(), pair);
	}

	private String ordersUrl() {
		return accountUrl() + "/orders";
	}

	private String tradesUrl() {
		return accountUrl() + "/trades";
	}

	public Order updateOrder(Order order) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + cfg.getString("token"));
		headers.set("Content-Type", "application/x-www-form-urlencoded");
		headers.set("X-Accept-Datetime-Format", "UNIX");
		headers.set("X-HTTP-Method-Override", "PATCH");
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("stopLoss", String.format("%.5f", order.getStopLoss()));
		map.put("price", String.format("%.5f", order.getPrice()));
		HttpEntity<Object> entity = new HttpEntity<>(map, headers);
		Optional<Order> response = getResponse(updateOrderUrl(order), HttpMethod.POST, entity, Order.class);
		return response.orElse(null);
	}

	private String updateOrderUrl(Order order) {
		return ordersUrl() + "/" + order.getId();
	}

	@Override
	public Trade updateTrade(Trade trade) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + cfg.getString("token"));
		headers.set("Content-Type", "application/x-www-form-urlencoded");
		headers.set("X-Accept-Datetime-Format", "UNIX");
		headers.set("X-HTTP-Method-Override", "PATCH");
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("stopLoss", String.format("%.5f", trade.getStopLoss()));
		HttpEntity<Object> entity = new HttpEntity<>(map, headers);
		Optional<Trade> response = getResponse(updateTradeUrl(trade), HttpMethod.POST, entity, Trade.class);
		return response.orElse(null);
	}

	private String updateTradeUrl(Trade trade) {
		return tradesUrl() + "/" + trade.getId();
	}

}
