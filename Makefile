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

BASE_DIR  :=$(shell pwd)
BUILD_DIR ?=$(BASE_DIR)/build
DL_DIR    ?=$(BASE_DIR)/dl
OUTPUT_DIR?=$(BASE_DIR)/output
REPO_DIR  ?=$(BASE_DIR)/bigtop-repos
DIST_DIR  ?=$(BASE_DIR)/dist

REQUIRED_DIRS = $(BUILD_DIR) $(DL_DIR) $(OUTPUT_DIR)
_MKDIRS :=$(shell for d in $(REQUIRED_DIRS); \
  do                               \
    [ -d $$d ] || mkdir -p $$d;  \
  done)

TARGETS:=
TARGETS_HELP:=
TARGETS_CLEAN:=


# Default Apache mirror
APACHE_MIRROR ?= http://apache.osuosl.org
APACHE_ARCHIVE ?= http://archive.apache.org/dist
CLOUDERA_ARCHIVE ?= http://archive.cloudera.com/tarballs/

# Include the implicit rules and functions for building packages
include package.mk
include bigtop.mk

help: package-help

all: srpm sdeb
world: all

packages: $(TARGETS)

help-header:
	@echo "    targets:"
	@echo "    all       (all TGZs/SRPMS/SDEBS)"
	@echo "    srpm      (all SRPMs)"
	@echo "    rpm       (all RPMs)"
	@echo "    sdeb      (all SDEBs)"
	@echo "    deb       (all DEBs)"
	@echo "    clean     (remove build/output dirs)"
	@echo "    realclean (remove build/output/dl dirs)"

package-help: help-header $(TARGETS_HELP)

clean: $(TARGETS_CLEAN)
	-rm -rf $(BUILD_DIR)
	-rm -rf $(OUTPUT_DIR)
	-rm -rf $(DIST_DIR)

realclean: clean
	-rm -rf $(DL_DIR)

srpm: $(TARGETS_SRPM)

rpm: $(TARGETS_RPM)

yum: $(TARGETS_YUM)

apt: $(TARGETS_APT)

sdeb: $(TARGETS_SDEB)

deb: $(TARGETS_DEB)

relnotes: $(TARGETS_RELNOTES)

checkenv:
	./check-env.sh

dist: realclean
	mkdir -p $(DIST_DIR)
	rsync -avz --exclude=.svn --exclude=.git --exclude=.gitignore --exclude=dist "$(BASE_DIR)/" "$(DIST_DIR)/bigtop-$(BIGTOP_VERSION)"
	cd $(DIST_DIR) && tar -cvzf "$(DIST_DIR)/bigtop-$(BIGTOP_VERSION).tar.gz" "bigtop-$(BIGTOP_VERSION)"

.DEFAULT_GOAL:= help
.PHONY: clean package-help help-header packages all world help srpm sdeb
