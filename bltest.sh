#! /bin/bash
curl -X POST -d @$1 -H "Content-Type:application/json" http://127.0.0.1:9000/rest/add/blacklist/goog

