package com.oanda.bot.repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.oanda.bot.domain.Candle;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.domain.Step;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class CandleRepository {

    private static final String serName = "candles.ser";

    private final Map<String, TreeMap<DateTime, Candle>> candles = Maps.newHashMap();

    public List<Candle> getCandles(String key) {
        return Lists.newArrayList(this.candles.getOrDefault(key, Maps.newTreeMap()).values());
    }

    public List<Candle> getCandles(Instrument instrument, Step step) {
        return Lists.newArrayList(this.candles.getOrDefault(getKey(instrument, step), Maps.newTreeMap()).values());
    }

    @Synchronized
    public void clearAllCandles() {
        this.candles.clear();
    }

    @Synchronized
    public void clearCandles(Instrument instrument, Step step) {
        this.candles.put(getKey(instrument, step), Maps.newTreeMap());
    }

    @Synchronized
    public void addCandles(Instrument instrument, Step step, List<Candle> candles) {
        String key = getKey(instrument, step);
        List<Candle> current = getCandles(key);
        current.addAll(candles);
        this.candles.put(key, getMapFromList(current));
    }

    @Synchronized
    public void addCandle(Instrument instrument, Step step, Candle candle) {
        String key = getKey(instrument, step);
        List<Candle> current = getCandles(key);
        current.add(candle);
        this.candles.put(key, getMapFromList(current));
    }

    @Synchronized
    public Candle getLastCandle(Instrument instrument, Step step) {
        List<Candle> current = getCandles(getKey(instrument, step));
        return current.isEmpty() ? null : current.get(current.size() - 1);
    }

    private String getKey(Instrument instrument, Step step) {
        return instrument.getInstrument() + step.name();
    }

    private TreeMap<DateTime, Candle> getMapFromList(List<Candle> current) {
        return current.stream().collect(Collectors.toMap(Candle::getTime, item -> item, (a, b) -> b, TreeMap::new));
    }
}
