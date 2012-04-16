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

# Implicit targets
SHELL := /bin/bash

# Download
$(BUILD_DIR)/%/.download:
	mkdir -p $(@D)
	mkdir -p $(DL_DIR)
	[ -z "$($(PKG)_TARBALL_SRC)" -o -f $($(PKG)_DOWNLOAD_DST) ] || (cd $(DL_DIR) && curl --retry 5 -# -L -k -o $($(PKG)_TARBALL_DST) $($(PKG)_DOWNLOAD_URL))
	touch $@

# Make source RPMs
$(BUILD_DIR)/%/.srpm:
	-rm -rf $(PKG_BUILD_DIR)/rpm/
	mkdir -p $(PKG_BUILD_DIR)/rpm/
	cp -r $(BASE_DIR)/bigtop-packages/src/rpm/$($(PKG)_NAME)/* $(PKG_BUILD_DIR)/rpm/
	mkdir -p $(PKG_BUILD_DIR)/rpm/{INSTALL,SOURCES,BUILD,SRPMS}
	[ -z "$($(PKG)_TARBALL_SRC)" ] || cp $($(PKG)_DOWNLOAD_DST) $(PKG_BUILD_DIR)/rpm/SOURCES
	[ -d $(BASE_DIR)/bigtop-packages/src/common/$($(PKG)_NAME) ] && cp -r $(BASE_DIR)/bigtop-packages/src/common/$($(PKG)_NAME)/* $(PKG_BUILD_DIR)/rpm/SOURCES
	PKG_NAME_FOR_PKG=$(subst -,_,$($(PKG)_NAME)); \
	rpmbuild --define "_topdir $(PKG_BUILD_DIR)/rpm/" \
						--define "$${PKG_NAME_FOR_PKG}_base_version $($(PKG)_BASE_VERSION)" \
						--define "$${PKG_NAME_FOR_PKG}_version $($(PKG)_PKG_VERSION)$(BIGTOP_BUILD_STAMP)" \
						--define "$${PKG_NAME_FOR_PKG}_release $($(PKG)_RELEASE_VERSION)%{?dist}" \
						-bs \
						--nodeps \
						--buildroot="$(PKG_BUILD_DIR)/rpm/INSTALL" \
						$(PKG_BUILD_DIR)/rpm/SPECS/$($(PKG)_NAME).spec
	mkdir -p $($(PKG)_OUTPUT_DIR)/
	$(PKG)_RELEASE_DIST=$(shell rpmbuild --eval '%{?dist}' 2>/dev/null); \
	cp $(PKG_BUILD_DIR)/rpm/SRPMS/$($(PKG)_PKG_NAME)-$($(PKG)_PKG_VERSION)$(BIGTOP_BUILD_STAMP)-$($(PKG)_RELEASE_VERSION)$${$(PKG)_RELEASE_DIST}.src.rpm \
	   $($(PKG)_OUTPUT_DIR)/
	touch $@

# Make binary RPMs
$(BUILD_DIR)/%/.rpm:
	$(PKG)_RELEASE_DIST=$(shell rpmbuild --eval '%{?dist}' 2>/dev/null); \
	SRCRPM=$($(PKG)_OUTPUT_DIR)/$($(PKG)_PKG_NAME)-$($(PKG)_PKG_VERSION)$(BIGTOP_BUILD_STAMP)-$($(PKG)_RELEASE_VERSION)$${$(PKG)_RELEASE_DIST}.src.rpm; \
	rpmbuild --define "_topdir $(PKG_BUILD_DIR)/rpm/" \
						--define "$($(PKG)_NAME)_base_version $($(PKG)_BASE_VERSION)" \
						--define "$($(PKG)_NAME)_version $($(PKG)_PKG_VERSION)$(BIGTOP_BUILD_STAMP)" \
						--define "$($(PKG)_NAME)_release $($(PKG)_RELEASE_VERSION)%{?dist}" \
						--rebuild $${SRCRPM}
	cp -r $(PKG_BUILD_DIR)/rpm/RPMS/*/* $($(PKG)_OUTPUT_DIR)/
	touch $@

# Make yum repo
$(BUILD_DIR)/%/.yum: $(BUILD_DIR)/%/.rpm
	createrepo -o $(OUTPUT_DIR) $(OUTPUT_DIR)
	touch $@

# Make source DEBs
$(BUILD_DIR)/%/.sdeb:
	-rm -rf $(PKG_BUILD_DIR)/deb/
	mkdir -p $(PKG_BUILD_DIR)/deb/$($(PKG)_NAME)-$(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP)
	# Only expands the tar if there is a source artifact
	if [ -n "$($(PKG)_TARBALL_SRC)" ]; then \
	  cp $($(PKG)_DOWNLOAD_DST) $(PKG_BUILD_DIR)/deb/$($(PKG)_PKG_NAME)_$(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP).orig.tar.gz ;\
	  cd $(PKG_BUILD_DIR)/deb/$($(PKG)_NAME)-$(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP) && \
	    tar --strip-components 1 -xvf ../$($(PKG)_PKG_NAME)_$(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP).orig.tar.gz;\
	else \
		 tar -czf $(PKG_BUILD_DIR)/deb/$($(PKG)_PKG_NAME)_$(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP).orig.tar.gz LICENSE ;\
	fi
	cd $(PKG_BUILD_DIR)/deb/$($(PKG)_NAME)-$(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP) && \
          cp -r $(BASE_DIR)/bigtop-packages/src/deb/$($(PKG)_NAME) debian && \
	  cp -r $(BASE_DIR)/bigtop-packages/src/common/$($(PKG)_NAME)/* debian && \
	  echo -e "version=$(PKG_PKG_VERSION)\ngit.hash=deadbeaf" >> debian/build.properties && \
	  (echo -e "$($(PKG)_PKG_NAME) ($(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP)-$($(PKG)_RELEASE)) stable; urgency=low\n" && \
           echo    "  Clean build" && \
           echo    " -- Bigtop <bigtop-dev@incubator.apache.org>  "`date +'%a, %d %b %Y %T %z'`) > debian/changelog && \
	  find debian -name "*.[ex,EX,~]" | xargs rm -f && \
	  dpkg-buildpackage -uc -us -sa -S
	mkdir -p $($(PKG)_OUTPUT_DIR)/
	for file in $($(PKG)_PKG_NAME)_$(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP)-$($(PKG)_RELEASE).dsc \
                    $($(PKG)_PKG_NAME)_$(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP)-$($(PKG)_RELEASE).diff.gz \
                    $($(PKG)_PKG_NAME)_$(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP)-$($(PKG)_RELEASE)_source.changes \
                    $($(PKG)_PKG_NAME)_$(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP).orig.tar.gz ; \
            do [ -e $(PKG_BUILD_DIR)/deb/$$file ] && cp $(PKG_BUILD_DIR)/deb/$$file $($(PKG)_OUTPUT_DIR); \
   done
	touch $@

$(BUILD_DIR)/%/.deb: SRCDEB=$($(PKG)_PKG_NAME)_$($(PKG)_PKG_VERSION)$(BIGTOP_BUILD_STAMP)-$($(PKG)_RELEASE).dsc
$(BUILD_DIR)/%/.deb:
	cd $($(PKG)_OUTPUT_DIR) && \
		dpkg-source -x $(SRCDEB) && \
		cd $($(PKG)_PKG_NAME)-$(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP) && \
			debuild \
				--preserve-envvar PATH \
				--preserve-envvar JAVA32_HOME \
				--preserve-envvar JAVA64_HOME \
				--preserve-envvar JAVA5_HOME \
				--preserve-envvar FORREST_HOME \
				--preserve-envvar MAVEN3_HOME \
				--preserve-envvar JAVA_HOME \
				--set-envvar=$(PKG)_BASE_VERSION=$($(PKG)_BASE_VERSION) \
				--set-envvar=$(PKG)_VERSION=$($(PKG)_PKG_VERSION)$(BIGTOP_BUILD_STAMP) \
				--set-envvar=$(PKG)_RELEASE=$($(PKG)_RELEASE_VERSION) \
				-uc -us -b
	rm -rf $($(PKG)_OUTPUT_DIR)/$($(PKG)_PKG_NAME)-$(PKG_PKG_VERSION)$(BIGTOP_BUILD_STAMP)
	touch $@

# Make apt repo
$(BUILD_DIR)/%/.apt: $(BUILD_DIR)/%/.deb
	-mkdir -p $(OUTPUT_DIR)/apt/conf
	cp $(REPO_DIR)/apt/distributions $(OUTPUT_DIR)/apt/conf
	for i in $($(PKG)_OUTPUT_DIR)/$($(PKG)_PKG_NAME)_$(PKG_PKG_VERSION)*.changes ; do reprepro -Vb $(OUTPUT_DIR)/apt include bigtop $$i ; done
	touch $@

# Package make function
# $1 is the target prefix, $2 is the variable prefix
define PACKAGE

# The default PKG_NAME will be the target prefix
$(2)_NAME           ?= $(1)

# For deb packages, the name of the package itself
$(2)_PKG_NAME       ?= $$($(2)_NAME)

# The default PKG_RELEASE will be 1 unless specified
$(2)_RELEASE        ?= 1

$(2)_BUILD_DIR      = $(BUILD_DIR)/$(1)/
$(2)_OUTPUT_DIR      = $(OUTPUT_DIR)/$(1)
$(2)_SOURCE_DIR       = $$($(2)_BUILD_DIR)/source

# Download source URL and destination path
$(2)_DOWNLOAD_URL = $($(2)_SITE)/$($(2)_TARBALL_SRC)
$(2)_DOWNLOAD_DST = $(DL_DIR)/$($(2)_TARBALL_DST)

# test that the download url will return http 200.  If it does not, use the ARCHIVE url instead of the MIRROR SITE url
ifneq ($$(shell curl -o /dev/null --silent --head --write-out '%{http_code}' $$($(2)_DOWNLOAD_URL)),200)
	$(2)_DOWNLOAD_URL = $($(2)_ARCHIVE)/$($(2)_TARBALL_SRC)
endif 

$(2)_TARGET_DL       = $$($(2)_BUILD_DIR)/.download
$(2)_TARGET_SRPM     = $$($(2)_BUILD_DIR)/.srpm
$(2)_TARGET_RPM      = $$($(2)_BUILD_DIR)/.rpm
$(2)_TARGET_YUM      = $$($(2)_BUILD_DIR)/.yum
$(2)_TARGET_SDEB     = $$($(2)_BUILD_DIR)/.sdeb
$(2)_TARGET_DEB      = $$($(2)_BUILD_DIR)/.deb
$(2)_TARGET_APT      = $$($(2)_BUILD_DIR)/.apt
$(2)_TARGET_RELNOTES = $$($(2)_BUILD_DIR)/.relnotes

# We download target when the source is not in the download directory
$(1)-download: $$($(2)_TARGET_DL)

# To make srpms, we need to build the package
$(1)-srpm: $(1)-download $$($(2)_TARGET_SRPM)

# To make binary rpms, we need to build source RPMs
$(1)-rpm: $(1)-srpm $$($(2)_TARGET_RPM)

# To make a yum/zypper repo, we need to build binary RPMs
$(1)-yum: $(1)-rpm $$($(2)_TARGET_YUM)

# To make sdebs, we need to build the package
$(1)-sdeb: $(1)-download $$($(2)_TARGET_SDEB)

# To make debs, we need to make source packages
$(1)-deb: $(1)-sdeb $$($(2)_TARGET_DEB)

# To make an apt repo, we need to build binary DEBs
$(1)-apt: $(1)-deb $$($(2)_TARGET_APT)

####
# Helper targets -version -help etc
$(1)-version:
	@echo "Base: $$($(2)_BASE_VERSION)"

$(1)-help:
	@echo "    $(1)  [$(1)-version, $(1)-info, $(1)-relnotes,"
	@echo "           $(1)-srpm, $(1)-rpm]"
	@echo "           $(1)-sdeb, $(1)-deb]"

$(1)-clean:
	rm -rf $(BUILD_DIR)/$(1)

$(1)-info:
	@echo "Info for package $(1)"
	@echo "  Will download from URL: $$($(2)_DOWNLOAD_URL)"
	@echo "  To destination file: $$($(2)_DOWNLOAD_DST)"
	@echo "  Then unpack into $$($(2)_SOURCE_DIR)"
	@echo
	@echo "Patches:"
	@echo "  BASE_REF: $$($(2)_BASE_REF)"
	@echo "  BUILD_REF: $$($(2)_BUILD_REF)"
	@echo
	@echo "Version: $$($(2)_BASE_VERSION)"
	@echo
	@echo "Stamp status:"
	@for mystamp in DL PREP PATCH BUILD SRPM RPM SDEB DEB RELNOTES;\
	  do echo -n "  $$$$mystamp: " ; \
	  ([ -f $($(1)_$$$$mystamp) ] && echo present || echo not present) ; \
	done

# Implicit rules with PKG variable
$$($(2)_TARGET_DL):       PKG=$(2)
$$($(2)_TARGET_APT) $$($(2)_TARGET_RPM) $$($(2)_TARGET_SRPM) $$($(2)_TARGET_SDEB) $$($(2)_TARGET_DEB): PKG=$(2)
$$($(2)_TARGET_RPM) $$($(2)_TARGET_SRPM) $$($(2)_TARGET_SDEB) $$($(2)_TARGET_DEB): PKG_BASE_VERSION=$$($(2)_BASE_VERSION)
$$($(2)_TARGET_APT) $$($(2)_TARGET_RPM) $$($(2)_TARGET_SRPM) $$($(2)_TARGET_SDEB) $$($(2)_TARGET_DEB): PKG_PKG_VERSION=$$($(2)_PKG_VERSION)
$$($(2)_TARGET_RPM) $$($(2)_TARGET_SRPM) $$($(2)_TARGET_SDEB) $$($(2)_TARGET_DEB): PKG_SOURCE_DIR=$$($(2)_SOURCE_DIR)
$$($(2)_TARGET_RPM) $$($(2)_TARGET_SRPM) $$($(2)_TARGET_SDEB) $$($(2)_TARGET_DEB): PKG_BUILD_DIR=$$($(2)_BUILD_DIR)


TARGETS += $(1)
TARGETS_HELP += $(1)-help
TARGETS_CLEAN += $(1)-clean
TARGETS_SRPM += $(1)-srpm
TARGETS_RPM += $(1)-rpm
TARGETS_SDEB += $(1)-sdeb
TARGETS_DEB += $(1)-deb
TARGETS_YUM += $(1)-yum
TARGETS_APT += $(1)-apt
endef
