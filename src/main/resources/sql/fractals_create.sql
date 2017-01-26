CREATE TABLE IF NOT EXISTS %FRACTALS%_%INSTRUMENT%_%STEP% (
	"time" timestamp NOT NULL, 
	direction int NOT NULL,
	broken boolean NULL,
	brokenTime timestamp NULL, 
	PRIMARY KEY ("time", direction)
)