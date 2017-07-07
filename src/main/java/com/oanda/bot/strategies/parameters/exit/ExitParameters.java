package com.oanda.bot.strategies.parameters.exit;

import com.oanda.bot.strategies.enums.ExitType;

public class ExitParameters {

	private ExitType exitType;
	
	private FixedStopLossParameters fixedStopLoss;

	private FixedStopGainParameters fixedStopGain;

	private TrailingStopGainParameters trailingStopGain;

	public ExitParameters() {
	}

	public ExitParameters(ExitType exitType, FixedStopLossParameters fixedStopLoss,
			FixedStopGainParameters fixedStopGain, TrailingStopGainParameters trailingStopGain) {
		this.exitType = exitType;
		this.fixedStopLoss = fixedStopLoss;
		this.fixedStopGain = fixedStopGain;
		this.trailingStopGain = trailingStopGain;
	}

	public ExitParameters(ExitType exitType, FixedStopLossParameters fixedStopLoss,
			FixedStopGainParameters fixedStopGain) {
		this.exitType = exitType;
		this.fixedStopLoss = fixedStopLoss;
		this.fixedStopGain = fixedStopGain;
		this.trailingStopGain = null;
	}

	public FixedStopLossParameters getFixedStopLoss() {
		return fixedStopLoss;
	}

	public FixedStopGainParameters getFixedStopGain() {
		return fixedStopGain;
	}

	public ExitType getExitType() {
		return exitType == null ? ExitType.ANY_INDICATOR : exitType;
	}

	public TrailingStopGainParameters getTrailingStopGain() {
		return trailingStopGain;
	}

}
