package com.oanda.bot.strategies.parameters.entry;

public class EntryParameters {

	private MovingAverageParameters movingAverageParameters;

	private RSIParameters rsiParameters;

	private BollingerBandsParameters bollingerBandsParameters;

	public EntryParameters() {
	}

	public EntryParameters(MovingAverageParameters movingAverageParameters, RSIParameters rsiParameters,
			BollingerBandsParameters bollingerBandsParameters) {
		this.movingAverageParameters = movingAverageParameters;
		this.rsiParameters = rsiParameters;
		this.bollingerBandsParameters = bollingerBandsParameters;
	}

	public MovingAverageParameters getMovingAverageParameters() {
		return movingAverageParameters;
	}

	public RSIParameters getRsiParameters() {
		return rsiParameters;
	}

	public BollingerBandsParameters getBollingerBandsParameters() {
		return bollingerBandsParameters;
	}

}
