SELECT "time" ,	
	openBid,
	highBid,
	lowBid,
	closeBid,
	openAsk,
	highAsk,
	lowAsk,
	closeAsk,
	volume ,
	complete 			
FROM %CANDLES%_%INSTRUMENT%_%STEP%
ORDER BY "time" DESC
limit 1