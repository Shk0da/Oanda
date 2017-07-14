INSERT INTO balance (
id,
currency,
"value"
) values (
:id,
:currency,
:balanceValue
) ON CONFLICT ("time", id) DO NOTHING;