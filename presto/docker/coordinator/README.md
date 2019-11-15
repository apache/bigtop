# Use squash !

Building this image, make sure you use squash so you can gut the 12 GB file :) 

docker build --squash -t bigtop/presto-coordinator:320 --build-arg PRESTO_IMAGE=prestosql/presto:320 .


