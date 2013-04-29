register $JAR_PATH

define HaversineDistInMiles datafu.pig.geo.HaversineDistInMiles();

data = LOAD 'input' AS (lat1:double,lng1:double,lat2:double,lng2:double);

data2 = FOREACH data GENERATE HaversineDistInMiles(lat1,lng1,lat2,lng2);

STORE data2 INTO 'output';
