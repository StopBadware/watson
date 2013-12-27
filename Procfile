web: target/universal/stage/bin/watson -Dhttp.port=$PORT $JAVA_OPTS
worker: java $WORKER_JAVA_OPTS -Dconfig.file=conf/application.conf -cp "target/universal/stage/lib/*" workers.Scheduler
