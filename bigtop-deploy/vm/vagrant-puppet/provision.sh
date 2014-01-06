# Install puppet agent
yum -y install http://yum.puppetlabs.com/puppetlabs-release-el-6.noarch.rpm
yum -y install puppet-2.7.23-1.el6.noarch

# Prepare puppet configuration file
cat > /bigtop-puppet/config/site.csv << EOF
hadoop_head_node,$1
hadoop_storage_dirs,/data/1,/data/2
bigtop_yumrepo_uri,http://bigtop.s3.amazonaws.com/releases/0.7.0/redhat/6/x86_64
jdk_package_name,java-1.7.0-openjdk-devel.x86_64
components,hadoop,hbase
EOF

mkdir -p /data/{1,2}
