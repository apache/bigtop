#!/bin/sh
rsync -a --delete bigtop-deploy/puppet/hieradata/site.yaml bigtop-deploy/puppet/hieradata/bigtop /etc/puppet/hieradata/
sudo puppet apply  -d --parser future --modulepath="bigtop-deploy/puppet/modules:/etc/puppet/modules" bigtop-deploy/puppet/manifests
