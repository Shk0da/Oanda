INSERT INTO %CANDLES%_%INSTRUMENT%_%STEP% (
	"time",
	openMid,
	highMid,
	lowMid,
	closeMid,
	volume ,
	complete 			
) values (
	:dateTime,
	:openMid,
	:highMid,
	:lowMid,
	:closeMid,
	:volume ,
	:complete 			
) ON CONFLICT ("time") DO NOTHING;