SELECT "time" ,	
	openMid,
	highMid,
	lowMid,
	closeMid,
	volume ,
	complete 			
FROM %CANDLES%_%INSTRUMENT%_%STEP%
WHERE "time" >= TO_TIMESTAMP(%FROMTIME% / 1000) AND "time" <= TO_TIMESTAMP(%TOTIME% / 1000)
ORDER BY "time" ASC;