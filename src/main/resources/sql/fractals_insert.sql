WITH ALL_CANDLES AS (
	SELECT 
		row_number() OVER () as rnum,
		"time",
		highAsk,
		lowBid
	FROM %CANDLES%_%INSTRUMENT%_%STEP%
	WHERE "time" >= COALESCE((SELECT max("time") FROM %FRACTALS%_%INSTRUMENT%_%STEP%), '1999-01-08')
	ORDER BY "time" DESC
),
MaxRnum AS (
	SELECT max(rnum) - 2 maxrnum FROM ALL_CANDLES 
),
fractalsHigh AS (
	SELECT 
		c.rnum,
		c."time",
		1 as direction
	FROM ALL_CANDLES c
		JOIN ALL_CANDLES l1 ON l1.rnum = c.rnum + 1 AND l1.highAsk < c.highAsk
		JOIN ALL_CANDLES r1 ON r1.rnum = c.rnum - 1 AND r1.highAsk < c.highAsk
		JOIN ALL_CANDLES l2 ON l2.rnum = c.rnum + 2 AND l2.highAsk < c.highAsk
		JOIN ALL_CANDLES r2 ON r2.rnum = c.rnum - 2 AND r2.highAsk < c.highAsk
	WHERE c.rnum > 2 AND c.rnum <= (SELECT maxrnum FROM MaxRnum)
	ORDER BY c."time" DESC
),
result AS (
	SELECT "time", direction FROM fractalsHigh
	UNION
	SELECT 
		c."time",
		-1 
	FROM ALL_CANDLES c
		JOIN ALL_CANDLES l1 ON l1.rnum = c.rnum + 1 AND l1.lowBid > c.lowBid
		JOIN ALL_CANDLES r1 ON r1.rnum = c.rnum - 1 AND r1.lowBid > c.lowBid
		JOIN ALL_CANDLES l2 ON l2.rnum = c.rnum + 2 AND l2.lowBid > c.lowBid
		JOIN ALL_CANDLES r2 ON r2.rnum = c.rnum - 2 AND r2.lowBid > c.lowBid
	WHERE c.rnum > 2 AND c.rnum <= (SELECT maxrnum FROM MaxRnum)
)
INSERT INTO %FRACTALS%_%INSTRUMENT%_%STEP% 
	SELECT "time", direction, null, null FROM result