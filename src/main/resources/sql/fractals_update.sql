WITH breaks AS (
	SELECT 
		row_number() OVER () as rnum,
		f."time", 
		f.direction,
		c.highMid,
		c.lowMid
	FROM %FRACTALS%_%INSTRUMENT%_%STEP% f
		JOIN %CANDLES%_%INSTRUMENT%_%STEP% c ON c."time"=f."time"
	WHERE f."time" > now() - interval '24 hours'
		COALESCE((SELECT max("time") FROM %FRACTALS%_%INSTRUMENT%_%STEP% WHERE broken = true),  '1999-01-08')
	ORDER BY "time" DESC
),
brokenFractals AS (
	select 
		b1."time", c."time" brokenTime
	from breaks b1
		join %CANDLES%_%INSTRUMENT%_%STEP% c on c."time" > b1."time"  
			and (b1.direction = 1 and c.highMid > b1.highMid 
				or b1.direction = -1 and c.lowMid < b1.lowMid)
				ORDER BY b1."time" desc
)
UPDATE %FRACTALS%_%INSTRUMENT%_%STEP% 
SET broken = true, brokenTime = bf.brokenTime
FROM brokenFractals bf 
WHERE %FRACTALS%_%INSTRUMENT%_%STEP%."time" = bf."time"



