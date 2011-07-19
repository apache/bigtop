#
# Run this script as testhiveuser, specifically for the testhive database.
# This script must be run after the mysql-create-db.sql has been run as root.
# Example of command to run this script:
# mysql testhive -u testhiveuser < /path/to/this/script.sql
#

#
# Drop test_table
#
drop table if exists test_table;

#
# Create test_table
#
create table test_table (a integer primary key, b varchar(32));

#
# Load table data
#
insert into test_table values (1, 'one'), (2, 'two'), (3, 'three'), (4, 'four'),
   (5, 'five'), (6, 'six'), (7, 'seven'), (8, 'eight'), (9, 'nine'), (10, 'ten'),
   (11, 'eleven'), (12, 'twelve');
