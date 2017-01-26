CREATE TABLE IF NOT EXISTS %CANDLES%_%INSTRUMENT%_%STEP% (
	"time" timestamp PRIMARY KEY, 
	openBid double precision,
	highBid double precision,
	lowBid double precision,
	closeBid double precision,
	openAsk double precision,
	highAsk double precision,
	lowAsk double precision,
	closeAsk double precision,
	volume integer,
	complete boolean	
)