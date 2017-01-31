package org.pminin.tb.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Instrument {

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Instruments {
		private List<Instrument> instruments = new ArrayList<Instrument>();
	}

	private String instrument;

	private String displayName;

	private double pip;

	private double maxTradeUnits;

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Instrument other = (Instrument) obj;
		if (instrument == null) {
			if (other.instrument != null)
				return false;
		} else if (!instrument.equals(other.instrument))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((instrument == null) ? 0 : instrument.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return instrument;
	}
}
