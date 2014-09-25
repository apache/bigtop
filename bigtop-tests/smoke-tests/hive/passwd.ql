drop table hivesmoke;
create external table hivesmoke (
field1 STRING
)
location '/tmp/hivesmoketest/';
select * from hivesmoke;
