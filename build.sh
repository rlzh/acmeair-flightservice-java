mvn clean package

docker build -t acmeair-flightservice-wl -f Dockerfile .
docker build -t acmeair-flightservice-ol -f Dockerfile-ol .
docker build -t acmeair-flightservice-wf -f Dockerfile-wf .
docker build -t acmeair-flightservice-pm -f Dockerfile-pm .

mvn clean package -f pom.xml_helidon
docker build -t acmeair-flightservice-pm -f Dockerfile-pm .

mvn clean package -f pom.xml_thorntail
docker build -t acmeair-flightservice-tt -f Dockerfile-tt .
