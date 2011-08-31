#!/usr/local/bin/perl

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


# get args
if (@ARGV < 3) {
	print STDERR "Usage: $0 base_name start stop max_test [ratings ...]\n";
	exit 1;
}
$basename = shift;
$start = shift;
$stop = shift;
$maxtest = shift;

# open files
open( TESTFILE, ">$basename.test" ) or die "Cannot open $basename.test for writing\n";
open( BASEFILE, ">$basename.base" ) or die "Cannot open $basename.base for writing\n";

# init variables
$testcnt = 0;

while (<>) {
	($user) = split;
	if (! defined $ratingcnt{$user}) {
		$ratingcnt{$user} = 0;
	}
	++$ratingcnt{$user};
	if (($testcnt < $maxtest || $maxtest <= 0)
	&& $ratingcnt{$user} >= $start && $ratingcnt{$user} <= $stop) {
		++$testcnt;
		print TESTFILE;
	}
	else {
		print BASEFILE;
	}
}
