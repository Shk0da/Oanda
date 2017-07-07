package com.oanda.bot.strategies.indicators.candles;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.CachedIndicator;
import eu.verdelhan.ta4j.indicators.candles.RealBodyIndicator;
import eu.verdelhan.ta4j.indicators.candles.UpperShadowIndicator;

public class DoubleTopIndicator  extends CachedIndicator<Boolean> {
	private final int timeFrame;
	private final Decimal factor;

	private UpperShadowIndicator upperShadowIndicator;
	private RealBodyIndicator realBodyIndicator;
	public DoubleTopIndicator(TimeSeries series, int timeFrame, Decimal factor){
		super(series);
		this.factor = factor;
		this.timeFrame = timeFrame;
		this.upperShadowIndicator = new UpperShadowIndicator(series);
		this.realBodyIndicator = new RealBodyIndicator(series);
	}
	@Override
	protected Boolean calculate(int index) {
		int idx = index-timeFrame;
		if(idx<=0){
			return false;
		}
		Decimal max = this.getTimeSeries().getTick(idx).getMaxPrice();
		for(int i=index-timeFrame;i<index;i++){
			Tick tick = this.getTimeSeries().getTick(i);
			if(max.isLessThan(tick.getMaxPrice())){
				max = tick.getMaxPrice();
				idx = i;
			}
		}
		Decimal shadow = this.upperShadowIndicator.getValue(idx);
		Decimal body = this.realBodyIndicator.getValue(idx);
		Tick tk = this.getTimeSeries().getTick(index);
		Decimal abs = tk.getClosePrice().minus(max).abs();
		
		
		if(shadow.isGreaterThanOrEqual(body) && abs.isLessThan(tk.getOpenPrice().multipliedBy(factor))){
			return true;
		}
		return false;
	}

}
