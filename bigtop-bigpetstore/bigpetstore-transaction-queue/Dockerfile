FROM centos
RUN yum update -y
RUN yum install -y java-1.7.0-openjdk
RUN yum install -y unzip
ADD build/distributions/bigpetstore-transaction-queue-1.0.zip /opt/
WORKDIR /opt/
RUN unzip -o bigpetstore-transaction-queue-1.0.zip
CMD /opt/bigpetstore-transaction-queue-1.0/bin/bigpetstore-transaction-queue 
