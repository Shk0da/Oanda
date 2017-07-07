package com.oanda.bot.strategies.indicators.candles;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.CachedIndicator;

public class BottomIndicator extends CachedIndicator<Boolean>{
	
	
	private final int timeFrame;
	
	public BottomIndicator(TimeSeries series, int timeFrame){
		super(series);
		this.timeFrame = timeFrame;
	}
	
	@Override
	protected Boolean calculate(int index) {
		
		int idx = index-timeFrame;
		if(idx<=0){
			return false;
		}
		
		Tick tick = this.getTimeSeries().getTick(index);
		
		Decimal c = tick.getClosePrice();
		
		Map<Decimal,Integer> map = new TreeMap<Decimal,Integer>();
		for(int i=index-timeFrame;i<index;i++){
			Tick t = this.getTimeSeries().getTick(i);
			
			if(map.containsKey(t.getOpenPrice())){
				map.put(t.getOpenPrice(), map.get(t.getOpenPrice())+1);
			}else{
				map.put(t.getOpenPrice(), 1);
			}
			/*if(map.containsKey(t.getMaxPrice())){
				map.put(t.getMaxPrice(), map.get(t.getMaxPrice())+1);
			}else{
				map.put(t.getMaxPrice(), 1);
			}*/
			if(map.containsKey(t.getMinPrice())){
				map.put(t.getMinPrice(), map.get(t.getMinPrice())+1);
			}else{
				map.put(t.getMinPrice(), 1);
			}
			if(map.containsKey(t.getClosePrice())){
				map.put(t.getClosePrice(), map.get(t.getClosePrice())+1);
			}else{
				map.put(t.getClosePrice(), 1);
			}
		}
		Decimal[] tp = new Decimal[map.keySet().size()];
		map.keySet().toArray(tp);
		if(tp.length>2){
			if(tp[1].isGreaterThanOrEqual(c)){
				return true;
			}
		}
		
		return false;
	}
	/**
	 * 使用 Map按key进行排序
	 * @param map
	 * @return
	 */
	public static Map<Decimal,Integer> sortMapByKey(Map<Decimal,Integer> map) {
		if (map == null || map.isEmpty()) {
			return null;
		}

		Map<Decimal,Integer> sortMap = new TreeMap<Decimal,Integer>(new MapKeyComparator());

		sortMap.putAll(map);

		return sortMap;
	}
}
