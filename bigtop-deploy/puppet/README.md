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

As an end to end example, you can follow the vagrant-puppet-vm recipes to see how to set up

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

expects configuration to live in hiera as specified by $confdir/hiera.yaml. An example
hiera.yaml as well as hiera configuration yaml files are provided with the bigtop classes. They
basically take the form:

<pre>
key: value
</pre>

with syntactic variations for hashes and arrays. Please consult the excellent puppet and hiera
documentation for details.

All configuration is done via such key value assignments in hierdata/site.yaml.  Any options
not defined there will revert to a default value defined in hieradata/cluster.yaml, with the
following exceptions (which are required):

* bigtop::hadoop\_head\_node: must be set to the FQDN of the name node of your
	cluster (which will also become its job tracker and gateway)

* bigtop::bigtop\_repo\_uri: uri of a repository containing packages for
	hadoop as built by Bigtop.

$confdir is the directory that puppet will look into for its configuration.  On most systems, 
this will be either /etc/puppet/ or /etc/puppetlabs/puppet/.  You may override this value by 
specifying --confdir=path/to/config/dir on the puppet command line.

cluster.yaml also serves as an example what parameters can be set and how they usually interact
between modules.

You can instruct the recipes to install ssh-keys for user hdfs to enable passwordless login
across the cluster. This is for test purposes only, so by default the option is turned off.

Files such as ssh-keys are imported from the master using the puppet:/// URL scheme. For this
to work, fileserver has to be enabled on the puppet master, the files module enabled and access
allowed in auth.conf. fileserver.conf should therefore contain e.g.:

<pre>
[files]
  path /etc/puppet/files
  allow *
</pre>

No changes are required to the default puppet 3 auth.conf.

For other options that may be set here, look for class parameters in the modules'
manifests/init.pp files. Any class parameter can be used as a hiera key if prefixed with the
module and class namespace. Module hue's server class will look for its parameter rm_host as
hue::server::rm_host in hiera.
Note that if hadoop::hadoop\_storage\_dirs is left unset, puppet will attempt to guess which
directories to use.

## Usage

- Make sure that the bigtop-deploy directory is available on every node of your cluster, and then
- Make sure all required puppet's modules are installed:

  gradle toolchain-puppetmodules

  or

  puppet apply --modulepath=<path_to_bigtop> -e "include bigtop_toolchain::puppet-modules"

  This will install the following module(s) for you:
  * [puppet stdlib module](https://forge.puppetlabs.com/puppetlabs/stdlib)
  * [puppet apt module](https://forge.puppetlabs.com/puppetlabs/apt) on Ubuntu, Debian only

And run the following on those nodes:

<pre>
# cp bigtop-deploy/puppet/hiera.yaml /etc/puppet
# mkdir -p /etc/puppet/hieradata
# rsync -a --delete bigtop-deploy/puppet/hieradata/bigtop/ /etc/puppet/hieradata/bigtop/
# cat > /etc/puppet/hieradata/site.yaml &lt;&lt; EOF
# bigtop::hadoop_head_node: "hadoopmaster.example.com"
# hadoop::hadoop_storage_dirs:
#   - "/data/1"
#   - "/data/2"
# bigtop::bigtop_repo_uri: "http://mirror.example.com/path/to/mirror/"
# EOF
# puppet apply -d --modulepath="bigtop-deploy/puppet/modules:/etc/puppet/modules" bigtop-deploy/puppet/manifests/site.pp
</pre>

When ignite-hadoop accelerator is deployed the client configs are placed under
/etc/hadoop/ignite.client.conf. All one needs to do to run Mapreduce jobs on ignite-hadoop grid
is to set HADOOP_CONF_DIR=/etc/hadoop/ignite.client.conf in the client session.

# Passwords

These classes are mostly used for regression testing. For ease of use they
contain insecure default passwords in a number of places. If you intend to use
them in production environments, make sure to track down all those places and
set proper passwords. This can be done using the corresponding hiera settings.
Some of these (but almost certainly not all!) are:

<pre>
hadoop::common_hdfs::hadoop_http_authentication_signature_secret
hadoop::httpfs::secret
</pre>

## Automatic password generation

Instead of explicitly setting passwords in hiera, they can be automatically
generated using a program called trocla. However, there are a number of caveats
with this approach at the moment:

* currently this only works for the HTTP and HTTPFS authentication signature
  secrets.
* trocla has to be installed beforehand as explained below.
* Installation from ruby gems needs Internet connectivity and for some
  dependency gems development packages such as a compiler. This can be avoided
  by using binary packages from the distribution if available.
* Puppet has to be used in a master/agent setup. With puppet apply it will not
  work for the HTTP signature secrets because they needs to be the same across
  hosts which trocla can only achieve if running on the master.
* The functionality is disabled by default and needs to be enabled explicitly.
  Without it, default passwords from the code or hiera are still used.

trocla needs to be installed on the master only. To do so, run the following:

<pre>
# gem install trocla
# puppet module install duritong/trocla
# puppet apply -e "class { 'trocla::config': manage_dependencies => false }"
</pre>

The trocla ruby gem pulls in highline, moneta and bcrypt. The bcrypt gem needs
ruby development packages (ruby.h) and a compiler.

Alternatively you can install your distributions' binary packages *before*
running gem install. On Debian those packages can be installed as follows:

<pre>
apt-get install ruby-highline ruby-moneta ruby-bcrypt
</pre>

This installation process is expected to get easier once operation system
packages have been created. This is actively underway for Debian. See Debian
bugs #777761 and #777906 for progress.

After installing the trocla gem as outlined above, the following test should
work:

<pre>
# puppet apply -e "file { '/tmp/test': content => trocla("test", "plain") }"
# cat /tmp/test
puGNOX-G%zYDKHet
</pre>

Now, automatic password generation can be activated in site.yaml using

<pre>
hadoop::generate_secrets: true
</pre>
