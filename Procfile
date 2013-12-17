web: target/universal/stage/bin/watson -Dhttp.port=$PORT $JAVA_OPTS
worker: java -Dconfig.file=conf/application.conf -Djsse.enableSNIExtension:false -cp "target/universal/stage/lib/*" workers.Scheduler
