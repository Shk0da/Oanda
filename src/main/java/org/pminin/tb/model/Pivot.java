package org.pminin.tb.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pivot {

	private static final Logger log = LoggerFactory.getLogger(Pivot.class);

	public static final int TREND_UP = 1;
	public static final int TREND_DOWN = -1;

	private DateTime time;
	private Instrument instrument;

	private double r3;
	private double r2;
	private double r1;
	private double pp;
	private double s1;
	private double s2;
	private double s3;
	private double m0;
	private double m1;
	private double m2;
	private double m3;
	private double m4;
	private double m5;

	public Date getDateTime() {
		return time.toDate();
	}

	private double getNearest(double value, double spread, int trend, List<Double> points) {
		double defaultValue = value + value * instrument.getPip() * 2 * trend;
		Predicate<Double> isComplete = p -> trend * (p - value) > spread + instrument.getPip();

		List<Double> list = points.stream().collect(Collectors.partitioningBy(isComplete)).get(true);
		if (list == null || list.isEmpty()) {
			return defaultValue;
		}
		Optional<Double> nearest = Optional.of(defaultValue);
		if (trend > 0) {
			nearest = list.stream().min(Comparator.naturalOrder());
		} else if (trend < 0) {
			nearest = list.stream().max(Comparator.naturalOrder());
		}
		if (!nearest.isPresent()) {
			log.error(String.format("Could not find nearest pivot for %.5f with direction %d and pivot list: %s", value,
					trend, this));
		}
		return nearest.orElse(defaultValue).doubleValue();
	}

	public double getNearestNoMiddle(double value, double spread, int trend) {
		List<Double> points = new ArrayList<>();
		points.add(r3);
		points.add(r2);
		points.add(r1);
		points.add(pp);
		points.add(s1);
		points.add(s2);
		points.add(s3);

		return getNearest(value, spread, trend, points);
	}

	public double getNearestWithMiddle(double value, double spread, int trend) {
		List<Double> points = new ArrayList<>();
		points.add(r3);
		points.add(m5);
		points.add(r2);
		points.add(m4);
		points.add(r1);
		points.add(m3);
		points.add(m2);
		points.add(pp);
		points.add(s1);
		points.add(m1);
		points.add(s2);
		points.add(m0);
		points.add(s3);

		return getNearest(value, spread, trend, points);
	}

	public void setDateTime(Date date) {
		time = new DateTime(date.getTime(), DateTimeZone.getDefault());
	}
}
