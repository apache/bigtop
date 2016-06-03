<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
# Testing Bigtop on various Clouds

[Cloud Weather Report (cwr)][CWR] enables charm authors and maintainers to run
health checks and benchmarks on multiple clouds using [Juju][].

When the cwr starts executing, it deploys a bundle or charm on the clouds chosen
by the author. It runs all the tests associated with each charm in each cloud it
deployed to. It also runs benchmarks on those clouds allowing charm authors to
see how their charms are performing on different clouds.

Results of the test runs are stored in local static html pages with a link
provided at the end of the run.

You will first need to set up [Juju][] and [CWR][], and bootstrap one or more
Juju controllers, such as on [AWS][] or [GCE][].  Then, you can run the
report with the `cwr` tool:

    cwr aws gce bigtop-tests/cloud-weather-report/hadoop-processing.yaml

A set of various Cloud Weather Reports are generated daily and can be viewed
[online](http://status.juju.solutions/recent).


[CWR]: https://github.com/juju-solutions/cloud-weather-report/
[Juju]: https://jujucharms.com/docs/stable/getting-started
[AWS]: https://jujucharms.com/docs/stable/config-aws
[GCE]: https://jujucharms.com/docs/stable/config-gce
