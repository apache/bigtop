# Puppet classes for deploying Hadoop

## Configuration

manifests/init.pp expects configuration to live in CSV at $confdir/config/site.csv, 
which takes the form

<pre>
key,value[,value2,value3]
</pre>

An example is provided at config/site.csv.example.  These values are loaded using 
puppet's extlookup() mechanism.

Any options not defined there will revert to a default value defined in 
manifests/cluster.pp, with the following exceptions (which are required):

* hadoop\_head\_node: must be set to the FQDN of the name node of your cluster (which will also
                    become its job tracker and gateway)
* bigtop\_yumrepo\_uri: uri of a repository containing packages for hadoop as built by Bigtop.
 
$confdir is the directory that puppet will look into for its configuration.  On most systems, 
this will be either /etc/puppet/ or /etc/puppetlabs/puppet/.  You may override this value by 
specifying --confdir=path/to/config/dir on the puppet command line.

For other options that may be set here, look for calls to extlookup() in manifests/cluster.pp.
Note that if hadoop\_storage\_dirs is left unset, puppet will attempt to guess which directories 
to use.

## Usage

Make sure that the bigtop-deploy directory is available on every node of your cluster, and then 
run the following on those nodes:

<pre>
# mkdir /etc/puppet/config
# cat > /etc/puppet/config/site.csv <&lt;EOF
# hadoop_head_node,hadoopmaster.example.com
# hadoop_storage_dirs,/data/1,/data/2
# bigtop_yumrepo_uri,http://mirror.example.com/path/to/mirror/
# EOF
# puppet -d --modulepath=bigtop-deploy/puppet/modules bigtop-deploy/puppet/manifests/site.pp
</pre>
