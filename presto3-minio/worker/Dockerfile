FROM wlan0/presto-benchmark-environment:coordinator 
COPY ./json /resources/nyc_taxi_data_3M.json
COPY minio.properties /resources/presto-server-302-e.11/etc/catalog/minio.properties
COPY config.properties /resources/presto-server-302-e.11/etc/config.properties 
CMD cat   /resources/presto-server-302-e.11/etc/config.properties ;  /resources/presto-server-302-e.11/bin/launcher start && cat /resources/presto-server-302-e.11/etc/config.properties && echo "Hardcoded to s3://minio:9000 , using 'minio' and 'minio123' as the access credentials..." && sleep 60 && cat /resources/presto-server-302-e.11/etc/config.properties &&  tail -f ./resources/presto-server-302-e.11/var/log/server.log
