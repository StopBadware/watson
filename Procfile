web: target/universal/stage/bin/watson -Dhttp.port=$PORT $JAVA_OPTS
worker: java $JAVA_OPTS -cp "target/universal/stage/lib/*" workers.Scheduler
