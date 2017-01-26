SELECT
	"time",
	R3,
	R2,
	R1,
	PP,
	S1,
	S2,
	S3,
	M0,
	M1,
	M2,
	M3,
	M4,
	M5		
FROM %PIVOT%_%INSTRUMENT%
ORDER BY "time" DESC
limit 1