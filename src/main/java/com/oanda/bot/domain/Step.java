package com.oanda.bot.domain;

public enum Step {
	S2("2 seconds"), M1("1 minute"), M5("5 minutes"), M10("10 minutes"), M15("15 minutes"), M30("30 minutes"), H1(
			"1 hour"), H4("4 hours"), D("1 day"), W("1 week");

	private final String readable;

    Step(String readable) {
        this.readable = readable;
	}

	public String toReadable() {
		return readable;
	}

}