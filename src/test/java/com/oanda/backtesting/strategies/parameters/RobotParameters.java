package com.oanda.backtesting.strategies.parameters;

import com.oanda.backtesting.strategies.enums.ExitType;
import com.oanda.backtesting.strategies.parameters.daytrade.DayTradeParameters;
import com.oanda.backtesting.strategies.parameters.entry.EntryParameters;
import com.oanda.backtesting.strategies.parameters.exit.ExitParameters;

public class RobotParameters  {

	private EntryParameters entryParameters;
	private ExitParameters exitParameters;
	private DayTradeParameters dayTradeParameters;

	public RobotParameters() {
	}

	public RobotParameters(EntryParameters entryParameters) {
		this(entryParameters, new ExitParameters(ExitType.ANY_INDICATOR, null, null), null);
	}

	public RobotParameters(EntryParameters entryParameters, DayTradeParameters dayTradeParameters) {
		this(entryParameters, new ExitParameters(ExitType.ANY_INDICATOR, null, null), dayTradeParameters);
	}

	public RobotParameters(EntryParameters entryParameters, ExitParameters exitParameters) {
		this(entryParameters, exitParameters, null);
	}

	public RobotParameters(EntryParameters entryParameters, ExitParameters exitParameters,
			DayTradeParameters dayTradeParameters) {
		this.entryParameters = entryParameters;
		this.exitParameters = exitParameters;
		this.dayTradeParameters = dayTradeParameters;
	}

	public int compareTo(RobotParameters o) {
		return 0;
	}

	public EntryParameters getEntryParameters() {
		return entryParameters;
	}

	public ExitParameters getExitParameters() {
		return exitParameters;
	}

	public DayTradeParameters getDayTradeParameters() {
		return dayTradeParameters;
	}

}
