CREATE TABLE IF NOT EXISTS %CANDLES%_%INSTRUMENT%_%STEP% (
	"time" timestamp PRIMARY KEY, 
	openMid double precision,
	highMid double precision,
	lowMid double precision,
	closeMid double precision,
	volume integer,
	complete boolean	
)