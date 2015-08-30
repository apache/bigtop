# Bigtop Util Puppet Module

This module provides common utility functions used by other Bigtop puppet modules.

## Testing

Test dependencies are managed by Bundler:

```bash
gem install bundler
```

To install the required gems for testing, `cd` into `bigtop-util` and use:

```bash
bundle install --path vendor/bundle
```

Now unit tests can be run using:

```bash
bundle exec rake spec
```
