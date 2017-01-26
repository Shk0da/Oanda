package org.pminin.tb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountDetails {
	private String accountId;
	private String accountName;
	private double balance;
	private double unrealizedPl;
	private double realizedPl;
	private double marginUsed;
	private double marginAvail;
	private double openTrades;
	private int openOrders;
	private double marginRate;
	private String accountCurrency;
}
