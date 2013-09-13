# WATSON

## Required Environment Variables
* MONGO_URL
* REDIS_URL

## MongoDB Indexes
### uris
* {"sha2_256":1},{unique:true}
* {"blacklisted":1}
* {"reversed_host":1}
* {"host":1}
* {"path":1}
### hosts
* {"host":1},{unique:true}
* {"ips.ip":1},{sparse:true}
### ips
* {"ip":1},{unique:true}
* {"asns.asn":1},{sparse:true}
### autonomous_systems
* {"asn":1},{unique:true}
* {"name":1}
* {"country":1}
