package com.oanda.bot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Instrument {

	private String displayName;
	private int displayPrecision;
	private double marginRate;
	private int maximumOrderUnits;
	private int maximumPositionSize;
	private double maximumTrailingStopDistance;
	private int minimumTradeSize;
	private double minimumTrailingStopDistance;
	private String name;
	private int pipLocation;
	private int tradeUnitsPrecision;
	private String type;

	public String getInstrument() {
		return name;
	}

	public String getDisplayName() {
		return displayName;
	}

	public double getPip() {
		return Math.pow(10, pipLocation);
	}

	public double getMaxTradeUnits() {
		return maximumOrderUnits;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Instrument other = (Instrument) obj;
		if (getInstrument() == null) {
			if (other.getInstrument() != null)
				return false;
		} else if (!getInstrument().equals(other.getInstrument()))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getInstrument() == null) ? 0 : getInstrument().hashCode());
		return result;
	}

	@Override
	public String toString() {
		return getInstrument();
	}

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Instruments {
        private List<Instrument> instruments = new ArrayList<>();
    }
}
