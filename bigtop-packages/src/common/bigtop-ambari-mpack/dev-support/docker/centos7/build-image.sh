echo -e "\033[32mRemoving image ambari:2.7.5\033[0m"
docker rmi ambari:2.7.5

echo -e "\033[32mBuilding image ambari:2.7.5\033[0m"
docker build -t ambari:2.7.5 .
