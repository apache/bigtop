BASE_DIR  :=$(shell pwd)
BUILD_DIR ?=$(BASE_DIR)/build
DL_DIR    ?=$(BASE_DIR)/dl
OUTPUT_DIR?=$(BASE_DIR)/output
REPO_DIR  ?=$(BASE_DIR)/src

REQUIRED_DIRS = $(BUILD_DIR) $(DL_DIR) $(OUTPUT_DIR)
_MKDIRS :=$(shell for d in $(REQUIRED_DIRS); \
  do                               \
    [ -d $$d ] || mkdir -p $$d;  \
  done)

TARGETS:=
TARGETS_HELP:=
TARGETS_CLEAN:=


# Default Apache mirror
APACHE_MIRROR ?= http://mirrors.ibiblio.org/apache
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

.DEFAULT_GOAL:= help
.PHONY: clean package-help help-header packages all world help srpm sdeb
