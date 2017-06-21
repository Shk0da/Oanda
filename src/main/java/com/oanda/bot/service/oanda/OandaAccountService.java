package com.oanda.bot.service.oanda;

import com.oanda.bot.model.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jersey.repackaged.com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.oanda.bot.constants.Step;
import com.oanda.bot.*;
import com.oanda.bot.model.Candle.Candles;
import com.oanda.bot.model.Instrument.Instruments;
import com.oanda.bot.model.Price.Prices;
import com.oanda.bot.model.Trade.Trades;
import com.oanda.bot.service.AccountService;
import com.oanda.bot.util.DateTimeUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.*;
import org.springframework.social.support.FormMapHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service("oandaAccountService")
@Scope("singleton")
public class OandaAccountService implements AccountService {

    private static final String ACCOUNT_DETAILS_API = "v3/accounts/%s";
    private static final String PRICES_API = "v3/accounts/%s/pricing?instruments=%s";
    private static final String CALENDAR_API = "labs/v1/calendar?instrument=%s&period=-%d";
    private static final String INSTRUMENTS_API = "v3/accounts/%s/instruments?instruments=%s_%s";
    private static final String INSTRUMENTS_API_1 = "v3/accounts/%s/instruments?instruments=%s";
    private static final String CANDLES_API = "v3/instruments/%s/candles?price=M&granularity=%s&from=%s&to=%s&includeFirst=%b";
    private static final String CANDLES_API_COUNT = "v3/instruments/%s/candles?price=M&granularity=%s&count=%d";

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
        return apiUrl() + String.format(CANDLES_API, instrument.getInstrument(), step.toString(),
                DateTimeUtil.rfc3339(start), DateTimeUtil.rfc3339(end), includeFirst);
    }

    private String candlesUrl(Step step, int count, Instrument instrument) {
        return apiUrl() + String.format(CANDLES_API_COUNT, instrument.getInstrument(), step.toString(), count);
    }

    @Override
    public Accounts getAccountDetails() {
        Optional<Accounts> response = getResponse(accountUrl(), HttpMethod.GET, headers(), Accounts.class);
        return response.orElse(new Accounts());
    }

    private Candles getCandles(Step step, DateTime start, DateTime end, Instrument instrument, boolean includeFirst) {
        String candlesUrl = candlesUrl(step, start, end, instrument, includeFirst);
        Optional<Candles> candles = getResponse(candlesUrl, HttpMethod.GET, headers(), Candles.class);
        return candles.orElse(new Candles(instrument.toString(), step, new ArrayList<>()));
    }

    @Override
    public Candles getCandles(Step step, DateTime start, Instrument instrument) {
        DateTime end = DateTime.now(DateTimeZone.getDefault()).minusSeconds(5);
        return getCandles(step, start, end, instrument, false);
    }

    private Candles getCandles(Step step, int count, Instrument instrument) {
        String candlesUrl = candlesUrl(step, count, instrument);
        Optional<Candles> response = getResponse(candlesUrl, HttpMethod.GET, headers(), Candles.class);
        return response.orElse(new Candles());
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
    public Order.Orders getOrders(Instrument instrument) {
        Optional<Order.Orders> response = getResponse(getOrdersUrl(instrument), HttpMethod.GET, headers(), Order.Orders.class);
        return response.orElse(new Order.Orders());
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
                double high = pivotCandle.getHighMid();
                double low = pivotCandle.getLowMid();
                double close = pivotCandle.getCloseMid();
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

    @Override
    public Price getPrice(Instrument instrument) {
        String url = apiUrl() + String.format(PRICES_API, accountId(), instrument.toString());
        Optional<Prices> response = getResponse(url, HttpMethod.GET, headers(), Prices.class);
        if (response.isPresent()) {
            Optional<Price> priceOpt = response.get().getPrices().stream().findFirst();
            return priceOpt.orElse(null);
        }
        return null;
    }

    private <T> Optional<T> getResponse(String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType) {
        T response = null;
        try {
            RestTemplate tmpl = new RestTemplate();
            if (HttpMethod.POST.equals(method) || HttpMethod.PATCH.equals(method)) {
                tmpl.getMessageConverters().add(new FormMapHttpMessageConverter());
                response = tmpl.postForObject(url, entity, responseType);
            } else if (HttpMethod.PUT.equals(method)) {
                tmpl.getMessageConverters().add(new FormMapHttpMessageConverter());
                tmpl.put(url, entity);
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

        return Optional.ofNullable(response);
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
        return new HttpEntity<>(headers);
    }

    private String instrumentsUrl(String pair) {
        return apiUrl() + String.format(INSTRUMENTS_API_1, accountId(), pair);
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

    private String tradesUrl() {
        return accountUrl() + "/trades";
    }

    @Override
    public Order createOrder(Order order) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + cfg.getString("token"));
        headers.set("X-Accept-Datetime-Format", "UNIX");
        headers.set("Content-Type", "application/json");
        headers.set("X-HTTP-Method-Override", "POST");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(orderFactory(order), headers);
        Optional<PostOrderResponse> response = getResponse(ordersUrl(), HttpMethod.POST, entity, PostOrderResponse.class);
        return response.orElse(new PostOrderResponse()).getOrderCreateTransaction();
    }

    public Order updateOrder(Order order) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + cfg.getString("token"));
        headers.set("Content-Type", "application/json");
        headers.set("X-Accept-Datetime-Format", "UNIX");
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

    @Override
    public void closeOrdersAndTrades(Instrument instrument) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + cfg.getString("token"));
        headers.set("X-Accept-Datetime-Format", "UNIX");
        HttpEntity<Object> entity = new HttpEntity<>(headers);

        Order.Orders orders = getOrders(instrument);
        orders.getOrders().stream().forEach(order ->
                getResponse(updateOrderUrl(order) + "/cancel", HttpMethod.PUT, entity, Map.class));

        Trades trades = getTrades(instrument);
        trades.getTrades().stream().forEach(trade ->
                getResponse(updateTradeUrl(trade) + "/close", HttpMethod.PUT, entity, Trade.class));
    }

    private String updateOrderUrl(Order order) {
        return ordersUrl() + "/" + order.getId();
    }

    @Override
    public Trade updateTrade(Trade trade) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + cfg.getString("token"));
        headers.set("Content-Type", "application/json");
        headers.set("X-Accept-Datetime-Format", "UNIX");
        headers.set("X-HTTP-Method-Override", "PUT");

        Map<String, Object> map = new HashMap<>();
        map.put("stopLoss", new Trade.Details(trade.getStopLoss()));
        HttpEntity<Object> entity = new HttpEntity<>(map, headers);
        Optional<Trade> response = getResponse(updateTradeUrl(trade), HttpMethod.PUT, entity, Trade.class);
        return response.orElse(new Trade());
    }

    private String updateTradeUrl(Trade trade) {
        return tradesUrl() + "/" + trade.getId() + "/orders";
    }

    @Override
    public List<CalendarEvent> getCalendarEvents(Instrument instrument, int futureHoursCount) {
        String url = apiUrl() + String.format(CALENDAR_API, instrument.toString(), futureHoursCount * 3600);
        Optional<CalendarEvent[]> response = getResponse(url, HttpMethod.GET, headers(), CalendarEvent[].class);
        return Arrays.asList(response.orElse(new CalendarEvent[]{}));
    }

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

        map.forEach((k, v) -> {
            if (v == null || v.toString().isEmpty()) {
                logger.debug(order.toString());
            }
        });

        map.put("order", orderData);

        return map;
    }

}
