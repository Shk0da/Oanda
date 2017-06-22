package com.oanda.backtesting.strategies.parameters.entry;

import com.oanda.backtesting.strategies.enums.MovingAverageType;

public class MovingAverageParameters {

	private MovingAverageType shortType;
	private MovingAverageType longType;
	private int shortPeriods;
	private int longPeriods;

	public MovingAverageParameters() {
	}

	public MovingAverageParameters(int shortPeriods, int longPeriods) {
		this(MovingAverageType.SIMPLE, shortPeriods, longPeriods);
	}

	public MovingAverageParameters(MovingAverageType shortType, MovingAverageType longType, int shortPeriods,
			int longPeriods) {
		this.shortType = shortType;
		this.longType = longType;
		this.shortPeriods = shortPeriods;
		this.longPeriods = longPeriods;
	}

	public MovingAverageParameters(MovingAverageType type, int shortPeriods, int longPeriods) {
		this(type, type, shortPeriods, longPeriods);
	}

	public MovingAverageType getShortType() {
		return shortType;
	}

	public MovingAverageType getLongType() {
		return longType;
	}

	public int getShortPeriods() {
		return shortPeriods;
	}

	public int getLongPeriods() {
		return longPeriods;
	}

}
