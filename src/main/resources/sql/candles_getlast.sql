SELECT "time" ,	
	openMid,
	highMid,
	lowMid,
	closeMid,
	volume ,
	complete 			
FROM %CANDLES%_%INSTRUMENT%_%STEP%
ORDER BY "time" DESC
limit 1