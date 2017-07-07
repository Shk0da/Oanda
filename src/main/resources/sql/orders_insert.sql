INSERT INTO %ORDERS%_%INSTRUMENT% (
"time",
id,
replacesOrderID,
"type",
units,
price,
stopLoss,
takeProfit,
closed,
closedTime,
closedPrice
)
VALUES (
  :dateTime,
  :id,
  :replacesOrderID,
  :orderType,
  :units,
  :price,
  :stopLoss,
  :takeProfit,
  FALSE,
  NULL,
  NULL
)