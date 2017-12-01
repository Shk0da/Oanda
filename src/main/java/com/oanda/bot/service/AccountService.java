package com.oanda.bot.service;

import com.google.common.collect.Maps;
import com.oanda.bot.domain.*;
import com.oanda.bot.util.DateTimeUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.http.*;
import org.springframework.social.support.FormMapHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class AccountService {

    private static final String ACCOUNT_DETAILS_API = "v3/accounts/%s";
    private static final String PRICES_API = "v3/accounts/%s/pricing?instruments=%s";
    private static final String CALENDAR_API = "labs/v1/calendar?instrument=%s&period=-%d";
    private static final String INSTRUMENTS_API = "v3/accounts/%s/instruments?instruments=%s_%s";
    private static final String INSTRUMENTS_API_1 = "v3/accounts/%s/instruments?instruments=%s";
    private static final String CANDLES_API = "v3/instruments/%s/candles?price=MAB&granularity=%s&from=%s&to=%s&includeFirst=%b";
    private static final String CANDLES_API_COUNT = "v3/instruments/%s/candles?price=MAB&granularity=%s&count=%d";

    private final Config config;

    public AccountService() {
        config = ConfigFactory.load().getConfig("account.oandaAccountService");
        DateTimeZone.setDefault(DateTimeZone.forID(config.getString("timeZone")));
    }

    //config

    private String accountId() {
        return config.getString("accountId");
    }

    private String apiUrl() {
        return config.getString("url");
    }

    //url

    private String accountUrl() {
        return apiUrl() + String.format(ACCOUNT_DETAILS_API, accountId());
    }

    private String candlesUrl(Step step, DateTime start, DateTime end, Instrument instrument, boolean includeFirst) {
        return apiUrl() + String.format(CANDLES_API, instrument.getInstrument(), step.toString(),
                DateTimeUtil.rfc3339(start), DateTimeUtil.rfc3339(end), includeFirst);
    }

    private String candlesUrl(Step step, int count, Instrument instrument) {
        return apiUrl() + String.format(CANDLES_API_COUNT, instrument.getInstrument(), step.toString(), count);
    }

    private String instrumentsUrl(String pair) {
        return apiUrl() + String.format(INSTRUMENTS_API_1, accountId(), pair);
    }

    private String getOrdersUrl(Instrument instrument) {
        return String.format(ordersUrl() + "?instrument=%s", instrument.toString());
    }

    private String instrumentsUrl(String left, String right) {
        return apiUrl() + String.format(INSTRUMENTS_API, accountId(), left, right);
    }

    private String streamURL(String pair) {
        return accountUrl() + "/pricing/stream?instruments=" + pair;
    }

    private String ordersUrl() {
        return accountUrl() + "/orders";
    }

    private String getTradesUrl(Instrument instrument) {
        return String.format(tradesUrl() + "?instrument=%s", instrument.toString());
    }

    private String tradesUrl() {
        return accountUrl() + "/trades";
    }

    private String updateOrderUrl(Order order) {
        return ordersUrl() + "/" + order.getId();
    }

    private String updateTradeUrl(Trade trade) {
        return tradesUrl() + "/" + trade.getId() + "/orders";
    }

    //api

    public Accounts getAccountDetails() {
        Optional<Accounts> response = getResponse(accountUrl(), HttpMethod.GET, headers(), Accounts.class);
        return response.orElse(new Accounts());
    }

    private Candle.Candles getCandles(Step step, DateTime start, DateTime end, Instrument instrument, boolean includeFirst) {
        String candlesUrl = candlesUrl(step, start, end, instrument, includeFirst);
        Optional<Candle.Candles> candles = getResponse(candlesUrl, HttpMethod.GET, headers(), Candle.Candles.class);
        return candles.orElse(new Candle.Candles(instrument.toString(), step, new ArrayList<>()));
    }

    public Candle.Candles getCandles(Instrument instrument, Step step, DateTime start) {
        DateTime end = DateTime.now(DateTimeZone.getDefault());
        return getCandles(step, start, end, instrument, false);
    }

    public Candle.Candles getCandles(Step step, DateTime start, DateTime end, Instrument instrument) {
        return getCandles(step, start, end, instrument, false);
    }

    private Candle.Candles getCandles(Step step, int count, Instrument instrument) {
        String candlesUrl = candlesUrl(step, count, instrument);
        Optional<Candle.Candles> response = getResponse(candlesUrl, HttpMethod.GET, headers(), Candle.Candles.class);
        return response.orElse(new Candle.Candles());
    }

    public Instrument getInstrument(String pair) {
        Optional<Instrument.Instruments> response = getResponse(instrumentsUrl(pair), HttpMethod.GET, headers(), Instrument.Instruments.class);
        if (response.isPresent()) {
            List<Instrument> instruments = response.get().getInstruments();
            return instruments.stream().findFirst().orElse(null);
        } else {
            return null;
        }
    }

    public Order.Orders getOrders(Instrument instrument) {
        Optional<Order.Orders> response = getResponse(getOrdersUrl(instrument), HttpMethod.GET, headers(), Order.Orders.class);
        return response.orElse(new Order.Orders());
    }

    public Price getPrice(Instrument instrument) {
        String url = apiUrl() + String.format(PRICES_API, accountId(), instrument.toString());
        Optional<Price.Prices> response = getResponse(url, HttpMethod.GET, headers(), Price.Prices.class);
        if (response.isPresent()) {
            Optional<Price> priceOpt = response.get().getPrices().stream().findFirst();
            return priceOpt.orElse(null);
        }
        return null;
    }

    public Trade.Trades getTrades(Instrument instrument) {
        Optional<Trade.Trades> response = getResponse(getTradesUrl(instrument), HttpMethod.GET, headers(), Trade.Trades.class);
        return response.orElse(new Trade.Trades());
    }

    public List<CalendarEvent> getCalendarEvents(Instrument instrument, int futureHoursCount) {
        String url = apiUrl() + String.format(CALENDAR_API, instrument.toString(), futureHoursCount * 3600);
        Optional<CalendarEvent[]> response = getResponse(url, HttpMethod.GET, headers(), CalendarEvent[].class);
        return Arrays.asList(response.orElse(new CalendarEvent[]{}));
    }

    public Order createOrder(Order order) {
        HttpHeaders headers = headers().getHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-HTTP-Method-Override", "POST");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderFactory(order), headers);
        Optional<PostOrderResponse> response = getResponse(ordersUrl(), HttpMethod.POST, entity, PostOrderResponse.class);
        return response.orElse(new PostOrderResponse()).getOrderCreateTransaction();
    }

    public Trade updateTrade(Trade trade) {
        HttpHeaders headers = headers().getHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-HTTP-Method-Override", "PUT");

        Map<String, Object> map = new HashMap<>();
        map.put("stopLoss", new Trade.Details(trade.getStopLoss()));
        HttpEntity<Object> entity = new HttpEntity<>(map, headers);
        Optional<Trade> response = getResponse(updateTradeUrl(trade), HttpMethod.PUT, entity, Trade.class);
        return response.orElse(new Trade());
    }

    public Order updateOrder(Order order) {
        HttpHeaders headers = headers().getHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-HTTP-Method-Override", "PUT");

        boolean orderHasBeenCanceled = true;
        String orderId = order.getId();
        Order.Orders orders = getOrders(getInstrument(order.getInstrument()));
        for (Order item : orders.getOrders()) {
            if (item.getId().equals(orderId)) {
                orderHasBeenCanceled = false;
            }
            if (item.getReplacesOrderID() != null && item.getReplacesOrderID().equals(orderId)) {
                order = item;
                orderHasBeenCanceled = false;
            }
        }

        if (orderHasBeenCanceled) {
            return createOrder(order);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderFactory(order), headers);
        Optional<Order> response = getResponse(updateOrderUrl(order), HttpMethod.PUT, entity, Order.class);
        return response.orElse(order);
    }

    public void closeOrdersAndTrades(Instrument instrument) {
        HttpEntity<Object> entity = new HttpEntity<>(headers());

        Order.Orders orders = getOrders(instrument);
        orders.getOrders().forEach(order ->
                getResponse(updateOrderUrl(order) + "/cancel", HttpMethod.PUT, entity, Map.class));

        Trade.Trades trades = getTrades(instrument);
        trades.getTrades().forEach(trade ->
                getResponse(tradesUrl() + "/" + trade.getId() + "/close", HttpMethod.PUT, entity, Trade.class));
    }

    //util

    private Map<String, Object> orderFactory(Order order) {
        Map<String, Object> orderData = Maps.newHashMap();
        orderData.put("timeInForce", order.getTimeInForce());
        orderData.put("gtdTime", String.valueOf(order.getGtdTime()));
        orderData.put("instrument", order.getInstrument());
        orderData.put("positionFill", order.getPositionFill());
        orderData.put("units", order.getUnits());
        orderData.put("type", order.getType().toString());
        orderData.put("price", String.format(Locale.ENGLISH, "%.5f", order.getPrice()));
        orderData.put("cancelledTime", order.getCancelledTime());

        /*takeProfitOnFill*/
        Map<String, String> takeProfitOnFill = Maps.newHashMap();
        takeProfitOnFill.put("timeInForce", order.getTakeProfitOnFill().getTimeInForce().toString());
        takeProfitOnFill.put("price", String.format(Locale.ENGLISH, "%.5f", order.getTakeProfitOnFill().getPrice()));
        orderData.put("takeProfitOnFill", takeProfitOnFill);

        /*stopLossOnFill*/
        Map<String, String> stopLossOnFill = Maps.newHashMap();
        stopLossOnFill.put("timeInForce", order.getStopLossOnFill().getTimeInForce().toString());
        stopLossOnFill.put("price", String.format(Locale.ENGLISH, "%.5f", order.getStopLossOnFill().getPrice()));
        orderData.put("stopLossOnFill", stopLossOnFill);

        Map<String, Object> map = Maps.newHashMap();
        map.put("order", orderData);

        return map;
    }

    private HttpEntity<Object> headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + config.getString("token"));
        headers.set("X-Accept-Datetime-Format", "UNIX");
        return new HttpEntity<>(headers);
    }

    private <T> Optional<T> getResponse(String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType) {
        T response = null;
        try {
            RestTemplate tmpl = new RestTemplate();
            if (HttpMethod.POST.equals(method) || HttpMethod.PATCH.equals(method)) {
                tmpl.getMessageConverters().add(new FormMapHttpMessageConverter());
                response = tmpl.postForObject(url, entity, responseType);
            /*} else if (HttpMethod.PUT.equals(method)) { TODO check this
                tmpl.getMessageConverters().add(new FormMapHttpMessageConverter());
                tmpl.put(url, entity);
                ResponseEntity<T> resp = tmpl.exchange(url, HttpMethod.PUT, entity, responseType);
                if (resp.getStatusCode() != HttpStatus.BAD_REQUEST && resp.hasBody()) {
                    response = resp.getBody();
                }*/
            } else {
                ResponseEntity<T> resp = tmpl.exchange(url, method, entity, responseType);
                if (resp.getStatusCode() == HttpStatus.OK && resp.hasBody()) {
                    response = resp.getBody();
                }
            }
        } catch (HttpStatusCodeException e) {
            log.error("Could not get response from " + url);
            log.error(e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Could not get response from " + url, e);
        }

        return Optional.ofNullable(response);
    }
}
