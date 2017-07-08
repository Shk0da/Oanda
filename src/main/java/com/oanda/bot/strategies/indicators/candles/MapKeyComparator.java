package com.oanda.bot.strategies.indicators.candles;

import eu.verdelhan.ta4j.Decimal;

import java.util.Comparator;

public class MapKeyComparator implements Comparator<Decimal>{

	@Override
	public int compare(Decimal decimal1, Decimal decimal2) {
		
		return decimal1.isLessThan(decimal2)?1:0;
	}
}