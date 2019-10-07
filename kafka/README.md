# This will use PVCs from a volume controller, similar to nifi.

Check the existing NIFI deployment for how to do the volume controller.  

`helm repo add incubator http://storage.googleapis.com/kubernetes-charts-incubator`
 
`helm install --name my-kafka incubator/kafka`
 
`helm delete --purge my-kafka`

# Reuse Zookeeper


To reuse zookeeper from the other examples, this helm chart needs to be modified
to inject zookeeper.  For an example of how to do that, first create the configmap inthis
directory.

From there, modify the statefulset : `kubectl edit statefulset my-kafka`, adding this stanza
to the end of the env declarations, which will cause it to reuse the ZK url specified inside 
of the kafka implementation.
```
        envFrom:
        - configMapRef:
            name: kafka-cm
```
