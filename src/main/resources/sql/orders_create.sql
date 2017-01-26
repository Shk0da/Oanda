CREATE TABLE IF NOT EXISTS %ORDERS%_%INSTRUMENT% (
	"time" timestamp NOT NULL,
  id varchar(20) NOT NULL,
  side varchar(20) NOT NULL,
  units double precision NOT NULL,
  price double precision NOT NULL,
  stopLoss double precision NOT NULL,
  takeProfit double precision NOT NULL,
  closed boolean NOT NULL,
  closedTime timestamp NULL,
  closedPrice double precision NULL,
  PRIMARY KEY ("time", id)
)
