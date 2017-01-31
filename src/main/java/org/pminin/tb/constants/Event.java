package org.pminin.tb.constants;

import org.pminin.tb.model.Candle;
import org.pminin.tb.model.Pivot;
import org.pminin.tb.model.Trade;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public enum Event {

	COLLECT_CANDLES, COLLECT_PIVOT, WORK, PROCESS_FRACTALS, NEWS_IN_5, TGI_FRIDAY, TREND_IS_HOT, KILL_EM_ALL, ORDER_CLOSED, TRADE_CLOSED, CHECK_TRADES_ORDERS;

	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class CandlesCollected {
		private Candle lastCompleteCandle;
		private Candle incompleteCandle;
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class CheckFractalBreak {
		Candle incompleteCandle;
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class CurrentRate {
		private Candle candle;
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class FractalBroken {
		private Step step;
		private Candle breakingCandle;
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class FractalConfirmed {
		private Step step;
		private Candle candle;
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class PivotChanged {
		private Pivot pivot;
	}

	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class TradeOpened {
		Trade trade;
	}

}
