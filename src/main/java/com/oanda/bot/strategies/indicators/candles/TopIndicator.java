package com.oanda.bot.strategies.indicators.candles;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.CachedIndicator;

import java.util.Map;
import java.util.TreeMap;

public class TopIndicator extends CachedIndicator<Boolean>{
	private final int timeFrame;
	public TopIndicator(TimeSeries series, int timeFrame){
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
			
			/*if(map.containsKey(t.getOpenPrice())){
				map.put(t.getOpenPrice(), map.get(t.getOpenPrice())+1);
			}else{
				map.put(t.getOpenPrice(), 1);
			}*/
			if(map.containsKey(t.getMaxPrice())){
				map.put(t.getMaxPrice(), map.get(t.getMaxPrice())+1);
			}else{
				map.put(t.getMaxPrice(), 1);
			}
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
		/*if(tp.length>6){
			if(c.isGreaterThan(tp[6])){
				return true;
			}else{
				return false;
			}
		}
		if(tp.length>5){
			if(c.isGreaterThan(tp[5])){
				return true;
			}else{
				return false;
			}
		}
		if(tp.length>4){
			if(c.isGreaterThan(tp[4])){
				return true;
			}else{
				return false;
			}
		}
		if(tp.length>3){
			if(c.isGreaterThan(tp[3])){
				return true;
			}else{
				return false;
			}
		}*/
		/*if(tp.length>2){
			if(c.isGreaterThan(tp[2])){
				return true;
			}else{
				return false;
			}
		}*/
		if(tp.length>1){
			if(c.isGreaterThan(tp[1])){
				return true;
			}else{
				return false;
			}
		}
		
		return false;
	}
}
