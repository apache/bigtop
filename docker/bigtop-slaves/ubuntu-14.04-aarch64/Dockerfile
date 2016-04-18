FROM aarch64/ubuntu
MAINTAINER Roman Shaposhnik <rvs@apache.org>

COPY bigtop_toolchain /etc/puppet/modules/bigtop_toolchain
COPY gradle.home /usr/share/gradle.home

RUN /etc/puppet/modules/bigtop_toolchain/bin/puppetize.sh
RUN puppet apply -e "include bigtop_toolchain::installer"
RUN chown jenkins:jenkins /usr/share/gradle.home

ENV GRADLE_USER_HOME=/usr/share/gradle.home

