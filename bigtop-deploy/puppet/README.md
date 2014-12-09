# Puppet classes for deploying Hadoop

## Intro

BigTop is now using Puppet 3.x (BIGTOP-1047)!

Installing and starting hadoop services is non-trivial, and for this reason bigtop 

packages puppet instrumentation for the various ecosystem components, which works

synergistically with bigtop produced software packages.

The puppet classes for bigtop deployment setup and deploy hadoop services.

This includes tasks such as:

- service installation
- pointing slaves to masters (i.e. regionservers, nodemanagers to their respective master)
- starting the services

The mode of puppet is masterless : there is no fancy coordination happening behind the scenes.

Puppet has a notion of a configuration directory, called config.  

When running puppet apply, note that puppet's confdir is *underneath* the --confdir value.

For example: 

If you have site.csv in /etc/puppet/config, 

Then you should use --confdir=/etc/puppet , and puppet finds the config dir underneath.

As an end to end example, you can follow the vagrant-puppet recipes to see how to set up 

a puppet managed bigtop hadoop installation.  Those examples are gauranteed to work and 

serve as a pedagogical round trip to the way bigtop integrates packaging, deployment, and 

testing all into one package.

## Debugging

If in any case, you need to debug these recipes, you can add notify("...") statements into 
the puppet scripts.  

In time, we will add more logging and debugging to these recipes.  Feel free to submit 

a patch for this !

## Configuration

As above, we defined a confdir (i.e. /etc/puppet/) which has a config/ directory in it.

The heart of puppet is the manifests file.  This file ( manifests/init.pp ) 

expects configuration to live in CSV at $confdir/config/site.csv, which takes the form

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

You can instruct the recipes to install ssh-keys for user hdfs to enable passwordless login
across the cluster. This is for test purposes only, so by default the option is turned off.
Refer to bigtop-deploy/puppet/config/site.csv.example for more details.

For other options that may be set here, look for calls to extlookup() in manifests/cluster.pp.
Note that if hadoop\_storage\_dirs is left unset, puppet will attempt to guess which directories 
to use.

## Usage

- Make sure that the bigtop-deploy directory is available on every node of your cluster, and then
- Make sure you've installed puppet's stdlib "puppet module install puppetlabs/stdlib".

And run the following on those nodes:

<pre>
# mkdir /etc/puppet/config
# cat > /etc/puppet/config/site.csv &lt;&lt; EOF
# hadoop_head_node,hadoopmaster.example.com
# hadoop_storage_dirs,/data/1,/data/2
# bigtop_yumrepo_uri,http://mirror.example.com/path/to/mirror/
# EOF
# puppet apply -d --modulepath=bigtop-deploy/puppet/modules bigtop-deploy/puppet/manifests/site.pp
</pre>
