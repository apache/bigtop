# Steps to setup sparkwith s3 interop

- `kubectl create configmap spark-conf --from-file=core-site.xml --from-file=log4j.properties --from-file=spark-defaults.conf --from-file=spark-env.sh -n bigdata` The first thing we do is create a configmap so we can mount these files directly into spark.
- `helm install microsoft/spark --version 1.0.0 --namespace=bigdata`
- Now, you need to *patch* the kubectl file so that it uses the volumeMounts as shown in the deployment.  
That is, you need to `kubectl edit deployment spark -n bigdata` , such that the */opt/spark/conf* directory is mounted over by the above `spark-conf` config map, like so:
```
        volumeMounts:
        - mountPath: /opt/spark/conf/
          name: spark-conf-vol
      dnsPolicy: ClusterFirst
      restartPolicy: Always
      schedulerName: default-scheduler
      securityContext: {}
      terminationGracePeriodSeconds: 30
      volumes:
      - configMap:
          defaultMode: 420
          name: spark-conf
        name: spark-conf-vol
```
