package org.pminin.tb.constants;

public enum Step {
	S2("2 seconds", "0/2 * * * * *"), M1("1 minute", "5 ? * * * *"), M5("5 minutes", "5 */5 * * * *"), M30("30 minutes",
			"5 */30 * * * *"), H1("1 hour", "5 0 ? * * *"), H4("4 hours",
					"5 0 */4 * * *"), D("1 day", "5 0 0 ? * *"), W("1 week", "5 0 0 */7 * *");

	private final String readable;

	private final String cron;

	private Step(String readable, String cron) {
		this.readable = readable;
		this.cron = cron;
	}

	public String toReadable() {
		return readable;
	}

	public String cron() {
		return cron;
	}
}