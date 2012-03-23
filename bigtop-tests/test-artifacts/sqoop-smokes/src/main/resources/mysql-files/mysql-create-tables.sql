/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* functional test db tables */
use mysqltestdb;

drop table if exists t_bool;
create table t_bool ( pri int not null auto_increment, a boolean, primary key (pri));

drop table if exists t_int;
create table t_int ( pri int not null auto_increment,a tinyint, b smallint, c mediumint , d int, e integer , f bigint,primary key (pri));

drop table if exists t_fp;
create table t_fp (pri int not null auto_increment, a decimal (6,4), b float (6,4), c double precision (6,4),d float (4),primary key (pri));

drop table if exists t_date;
create table t_date (pri int not null auto_increment,a date , c timestamp, d time , e year(4),primary key (pri));

drop table if exists t_string;
create table t_string (pri int not null auto_increment, a char (5), b varchar (5), c text , d tinytext, e mediumtext, f mediumtext, g longtext, h enum ('A','B'),primary key (pri));

drop table if exists testtable;
create table testtable ( id int , fname varchar (20), lname varchar(20) , primary key (id)); 

drop table if exists testtable2;
create table testtable2 ( id int , fname varchar (20), lname varchar(20) , primary key (id));

drop table if exists testnullvalues;
create table testnullvalues ( id int , a int , b varchar(20) , primary key (id));

/* import-all test db-tables */

use mysqltestdb2;

drop table if exists testtable;
create table testtable ( id int , fname varchar (20), lname varchar(20) , primary key (id)); 

drop table if exists testtable2;
create table testtable2 ( id int , fname varchar (20), lname varchar(20) , primary key (id));
