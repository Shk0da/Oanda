package com.oanda.bot.repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.oanda.bot.domain.Candle;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.domain.Step;
import com.oanda.bot.util.StockDataSetIterator;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class CandleRepository {

    @Getter
    @Value("${candle.repository.limit}")
    private Integer limit;

    private final Map<String, TreeMap<DateTime, Candle>> candles = Maps.newHashMap();

    public List<Candle> getCandles(String key) {
        return Lists.newArrayList(this.candles.getOrDefault(key, Maps.newTreeMap()).values());
    }

    public List<Candle> getCandles(Instrument symbol, Step step) {
        return Lists.newArrayList(this.candles.getOrDefault(getKey(symbol, step), Maps.newTreeMap()).values());
    }

    @Synchronized
    public void clearAllCandles() {
        this.candles.clear();
    }

    @Synchronized
    public void clearCandles(Instrument symbol, Step step) {
        this.candles.put(getKey(symbol, step), Maps.newTreeMap());
    }

    @Synchronized
    public void addCandles(Instrument symbol, Step step, List<Candle> candles) {
        String key = getKey(symbol, step);
        List<Candle> current = getCandles(key);
        current.addAll(candles);
        this.candles.put(key, getMapFromList(current));
    }

    @Synchronized
    public Candle getLastCandle(Instrument symbol, Step step) {
        List<Candle> current = getCandles(getKey(symbol, step));
        return current.isEmpty() ? null : current.get(current.size() - 1);
    }

    @Synchronized
    public List<Candle> getLastCandles(Instrument symbol, Step step, int size) {
        List<Candle> current = getCandles(getKey(symbol, step));

        if (current.isEmpty() || current.size() < StockDataSetIterator.VECTOR_SIZE) {
            return current;
        }

        if (size > current.size()) {
            size = current.size() - 1;
        }

        int fromIndex = current.size() - size;
        if (fromIndex < 0) {
            return current;
        }

        return current.subList(fromIndex, current.size());
    }

    public Integer getSize(Instrument symbol, Step step) {
        return getCandles(getKey(symbol, step)).size();
    }

    private String getKey(Instrument symbol, Step step) {
        return symbol.getInstrument() + step.name();
    }

    private TreeMap<DateTime, Candle> getMapFromList(List<Candle> current) {
        if (current.size() > limit * 1.2) {
            current = current.subList(current.size() - limit, current.size());
        }

        return current.stream()
                .filter(candle -> candle.getCloseMid() > 0)
                .collect(Collectors.toMap(Candle::getTime, item -> item, (a, b) -> b, TreeMap::new));
    }
}
