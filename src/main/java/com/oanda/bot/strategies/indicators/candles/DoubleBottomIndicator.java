package com.oanda.bot.strategies.indicators.candles;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.CachedIndicator;
import eu.verdelhan.ta4j.indicators.candles.LowerShadowIndicator;
import eu.verdelhan.ta4j.indicators.candles.RealBodyIndicator;

public class DoubleBottomIndicator  extends CachedIndicator<Boolean> {
	private final int timeFrame;
	private final Decimal factor;

	private LowerShadowIndicator lowerShadowIndicator;
	private RealBodyIndicator realBodyIndicator;
	public DoubleBottomIndicator(TimeSeries series, int timeFrame, Decimal factor){
		super(series);
		this.factor = factor;
		this.timeFrame = timeFrame;
		this.lowerShadowIndicator = new LowerShadowIndicator(series);
		this.realBodyIndicator = new RealBodyIndicator(series);
	}
	@Override
	protected Boolean calculate(int index) {
		int idx = index-timeFrame;
		if(idx<=0){
			return false;
		}
		Decimal min = this.getTimeSeries().getTick(idx).getMinPrice();
		for(int i=index-timeFrame;i<index;i++){
			Tick tick = this.getTimeSeries().getTick(i);
			if(min.isGreaterThan(tick.getMinPrice())){
				min = tick.getMinPrice();
				idx = i;
			}
		}
		Decimal shadow = this.lowerShadowIndicator.getValue(idx);
		Decimal body = this.realBodyIndicator.getValue(idx);
		Tick tk = this.getTimeSeries().getTick(index);
		Decimal abs = tk.getClosePrice().minus(min).abs();
		
		
		if(shadow.isGreaterThanOrEqual(body) && abs.isLessThan(tk.getOpenPrice().multipliedBy(factor))){
			return true;
		}
		return false;
	}

}
