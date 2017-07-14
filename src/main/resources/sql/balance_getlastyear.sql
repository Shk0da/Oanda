SELECT
  "time",
  "value"
FROM balance
WHERE id = '%ACCOUNTID%' AND "time" >= DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '1 year'
ORDER BY "time" ASC;