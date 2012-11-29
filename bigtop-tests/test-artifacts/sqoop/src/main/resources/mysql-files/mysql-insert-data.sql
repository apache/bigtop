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

use mysqltestdb;

insert into t_int (a,b,c,d,e,f) values (127,32767,8388607,2147483647,-2147483648,9223372036854775807);

insert into t_bool (a) values (1);

insert into t_fp(a,b,c,d) values (-99.9999,-99.9999,99.9999,99.99);

insert into t_date (a,c,d,e) values  ('9999-12-31',19731230153000,'23:59:59','2155');

insert into t_string (a,b,c,d,e,f,g,h) values ('aaa','aaa','0GIQM3CONg5jAUloRoP7f76TJrYkYnto5i76IUDbKuG1dMSP5znIM6Ct3qc1WdRCD9THFjWXBIxOoAqhOgdFQEwICX7fYN4n9P3AOoo3ZrnUBu3rtuwf9Y2HXJvUBgAqs44Ypdg6iv511JQSufMeKvbLIpIbk9saJW82EeTgoWkAS7lhI0HJXmvyDkJEqOc0VxdT0ySR','0GIQM3CONg5jAUloRoP7f76TJrYkYnto5i76IUDbKuG1dMSP5znIM6Ct3qc1WdRCD9THFjWXBIxOoAqhOgdFQEwICX7fYN4n9P3AOoo3ZrnUBu3rtuwf9Y2HXJvUBgAqs44Ypdg6iv511JQSufMeKvbLIpIbk9saJW82EeTgoWkAS7lhI0HJXmvyDkJEqOc0VxdT0ySR','0GIQM3CONg5jAUloRoP7f76TJrYkYnto5i76IUDbKuG1dMSP5znIM6Ct3qc1WdRCD9THFjWXBIxOoAqhOgdFQEwICX7fYN4n9P3AOoo3ZrnUBu3rtuwf9Y2HXJvUBgAqs44Ypdg6iv511JQSufMeKvbLIpIbk9saJW82EeTgoWkAS7lhI0HJXmvyDkJEqOc0VxdT0ySR','0GIQM3CONg5jAUloRoP7f76TJrYkYnto5i76IUDbKuG1dMSP5znIM6Ct3qc1WdRCD9THFjWXBIxOoAqhOgdFQEwICX7fYN4n9P3AOoo3ZrnUBu3rtuwf9Y2HXJvUBgAqs44Ypdg6iv511JQSufMeKvbLIpIbk9saJW82EeTgoWkAS7lhI0HJXmvyDkJEqOc0VxdT0ySR','0GIQM3CONg5jAUloRoP7f76TJrYkYnto5i76IUDbKuG1dMSP5znIM6Ct3qc1WdRCD9THFjWXBIxOoAqhOgdFQEwICX7fYN4n9P3AOoo3ZrnUBu3rtuwf9Y2HXJvUBgAqs44Ypdg6iv511JQSufMeKvbLIpIbk9saJW82EeTgoWkAS7lhI0HJXmvyDkJEqOc0VxdT0ySR','A');

insert into testtable values (1,'aaa','aaa');
insert into testtable values (2,'bbb','bbb');
insert into testtable values (3,'ccc','ccc');
insert into testtable values (4,'ddd','ddd');
insert into testtable values (5,'eee','eee');
insert into testtable values (6,'fff','fff');
insert into testtable values (7,'ggg','ggg');
insert into testtable values (8,'hhh','hhh');
insert into testtable values (9,'iii','iii');
insert into testtable values (10,'jjj','jjj');


insert into testtable2 values (1,'111','111');
insert into testtable2 values (2,'222','222');
insert into testtable2 values (3,'333','333');


insert into testnullvalues (id) values (1);
insert into testnullvalues (id,a) values (2,2);
insert into testnullvalues (id,b) values (3,'aaa');

/* data for import-all test */

use mysqltestdb2;

insert into testtable values (1,'aaa','aaa');
insert into testtable2 values (1,'aaa','aaa');
