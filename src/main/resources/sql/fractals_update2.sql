UPDATE %FRACTALS%_%INSTRUMENT%_%STEP% 
SET broken = true, brokenTime = :breakingTime
WHERE "time" = :fractalTime AND direction = :dir



