package com.oanda.backtesting.strategies.parameters.daytrade;

import java.time.LocalTime;

public class DayTradeParameters {

	private LocalTime initialEntryTimeLimit;

	private LocalTime finalEntryTimeLimit;

	private LocalTime exitTimeLimit;

	public DayTradeParameters() {
	}

	public DayTradeParameters(LocalTime initialEntryTimeLimit, LocalTime finalEntryTimeLimit, LocalTime exitTimeLimit) {
		this.initialEntryTimeLimit = initialEntryTimeLimit;
		this.finalEntryTimeLimit = finalEntryTimeLimit;
		this.exitTimeLimit = exitTimeLimit;
	}

	public LocalTime getInitialEntryTimeLimit() {
		return initialEntryTimeLimit;
	}

	public LocalTime getFinalEntryTimeLimit() {
		return finalEntryTimeLimit;
	}

	public LocalTime getExitTimeLimit() {
		return exitTimeLimit;
	}

}
