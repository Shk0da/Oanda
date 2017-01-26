package org.pminin.tb.util;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.pminin.tb.model.Candle;
import org.pminin.tb.model.Candle.Candles;

public class ModelUtil {

	private static List<Candle> getCandles(Candles candles, boolean completed) {
		List<Candle> list = candles.getCandles();

		Predicate<Candle> isComplete = p -> p.isComplete();

		Map<Boolean, List<Candle>> splittedCandles = list.stream().collect(Collectors.partitioningBy(isComplete));

		return splittedCandles.get(completed);
	}

	public static List<Candle> getCompletedCandles(Candles candles) {
		return getCandles(candles, true);
	}

	public static List<Candle> getNotCompletedCandles(Candles candles) {
		return getCandles(candles, false);
	}

}
