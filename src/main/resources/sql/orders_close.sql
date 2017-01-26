UPDATE %ORDERS%_%INSTRUMENT%
SET closed = TRUE, closedPrice = :closedPrice, closedTime = :closedTime
WHERE id = :id
