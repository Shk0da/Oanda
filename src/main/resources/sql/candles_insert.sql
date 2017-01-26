INSERT INTO %CANDLES%_%INSTRUMENT%_%STEP%  (
	"time" ,	
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
) values (
	:dateTime ,	
	:openBid,
	:highBid,
	:lowBid,
	:closeBid,
	:openAsk,
	:highAsk,
	:lowAsk,
	:closeAsk,
	:volume ,
	:complete 			
)