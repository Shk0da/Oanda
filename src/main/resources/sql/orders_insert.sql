INSERT INTO %ORDERS%_%INSTRUMENT%  (
	"time" ,
	id,
  side,
	units,
  price,
  stopLoss,
  takeProfit,
  closed,
	closedTime,
  closedPrice,
) values (
	:dateTime ,
	:id,
	:side,
	:units
	:price,
	:stopLoss,
	:takeProfit,
	FALSE,
	NULL,
	NULL
)
