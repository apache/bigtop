Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

# Bigtop Util Puppet Module

This module provides common utility functions used by other Bigtop puppet modules.

## Testing

Test dependencies are managed by Bundler:

```bash
gem install bundler
```

To install the required gems for testing, `cd` into `bigtop-util` and use:

```bash
bundle install --path vendor/bundle
```

Now unit tests can be run using:

```bash
bundle exec rake spec
```
