#
# Run this script once as root user before the test run. For example:
# mysql -u root -p < /path/to/this/script.sql
#

#
# Drop old databases
#
drop database if exists testhive;

#
# Create new database
#
create database testhive;

#
# Grant permissions to the testhiveuser
#
use mysql;
grant all privileges on testhive.* to 'testhiveuser'@'localhost';
grant all privileges on testhive.* to 'testhiveuser'@'%';
grant all privileges on testhive.* to 'root'@'%';
flush privileges;
