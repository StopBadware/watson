web: target/universal/stage/bin/watson -Dhttps.port=$PORT -Dhttps.keyStore=keystore.jks -Dhttps.keyStorePassword=password $JAVA_OPTS
worker: java $WORKER_JAVA_OPTS -Dconfig.file=conf/application.conf -cp "target/universal/stage/lib/*" workers.Scheduler
