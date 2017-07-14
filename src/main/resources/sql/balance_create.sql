CREATE TABLE IF NOT EXISTS balance (
  "time"   TIMESTAMP NOT NULL DEFAULT NOW(),
  id       VARCHAR(254),
  currency VARCHAR(254),
  value    DOUBLE PRECISION,
  PRIMARY KEY ("time", id)
)