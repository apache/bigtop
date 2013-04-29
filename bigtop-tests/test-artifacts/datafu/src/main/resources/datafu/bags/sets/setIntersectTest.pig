register $JAR_PATH

define SetIntersect datafu.pig.bags.sets.SetIntersect();

data = LOAD 'input' AS (B1:bag{T:tuple(val1:int,val2:int)},B2:bag{T:tuple(val1:int,val2:int)});

data2 = FOREACH data GENERATE SetIntersect(B1,B2);

STORE data2 INTO 'output';
