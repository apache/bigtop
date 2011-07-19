#
# Run this script once as root user before the test run. For example:
# mysql -u root -p < /path/to/this/script.sql
#

#
# Drop old databases
#
drop database if exists testhbase;

#
# Create new database
#
create database testhbase;

#
# Grant permissions to the testhbaseuser
#
use mysql;
grant all privileges on testhbase.* to 'testhbaseuser'@'localhost';
grant all privileges on testhbase.* to 'testhbaseuser'@'%';
grant all privileges on testhbase.* to 'root'@'%';
flush privileges;
