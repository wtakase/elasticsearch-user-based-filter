User based filter plugin for ElasticSearch
====

## Overview

This plugin restricts ElasticSearch API requests from Kibana based on REMOTE_USER.


## Requirement

 * ElasticSearch = 2.0.0
 * Apache Maven

## Install

```bash
# cd elasticsearch-user-based-filter
# mvn clean package
# /usr/share/elasticsearch/bin/plugin remove user-based-filter
# /usr/share/elasticsearch/bin/plugin install file://`pwd`/target/releases/user-based-filter-2.0.0.zip
# systemctl restart elasticsearch
```

## Usage

 * Configure Apache HTTP server in order to send `REMOTE_USER` parameter to ElasticSearch (see `elasticsearch-user-based-filter.conf.sample`)
 * Prepare `/etc/elasticsearch/user-based-filter/usermap` (see `usermap.sample`)
 * Put `user-based-filter.properties` under `/etc/elasticsearch/user-based-filter/`

## Note

 * Current plugin only filters Get/Multi Get/Search/Multi Search APIs by overriding the Rest Action classes.
 * In order to filter wider APIs, it is better to override NettyHttpRequest and modify content and params at initialization.

## Lincence

GPL

## Acknowledgements

A part of this work was accomplished at [CERN IT-OIS-CV group](http://information-technology.web.cern.ch/about/organisation/cloud-and-virtualisation).

## Author

[wtakase](https://github.com/wtakase)
