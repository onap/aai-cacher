# Cacher Microservice 


Build and test
````bash
	mvn clean install
````

Run spring boot
```bash
	mvn spring-boot:run
```

if on Windows the startSpring.bat file can be used to start the mS with the debug port configured

The purpose of this microservice is to provide the ability to store and force update cached responses as well as for particular cases update the cached data from consuming dmaap events.

##Cache Key Inventory API endpoints

localhost:8444/aai/cacheKey/get

localhost:8444/aai/cacheKey/delete

localhost:8444/aai/cacheKey/add

localhost:8444/aai/cacheKey/update

##Cache API endpoints

localhost:8444/aai/cache/delete

localhost:8444/aai/cache/sync

localhost:8444/aai/cache/get




