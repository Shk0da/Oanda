package com.oanda.bot.strategies.parameters.entry;

import eu.verdelhan.ta4j.Decimal;

public class BollingerBandsParameters {

	private int periods;

	private Decimal factor;

	public BollingerBandsParameters() {
	}

	public BollingerBandsParameters(int periods, Decimal factor) {
		this.periods = periods;
		this.factor = factor;
	}

	public int getPeriods() {
		return periods;
	}

	public Decimal getFactor() {
		return factor;
	}

}
