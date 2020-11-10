# Task API
This is an API that can be implemented to create a scalable distributed streaming service. To test the example implementation follow the steps in Quick Start.

## Quick Start
1. ```sbt docker:publishLocal```
2. ```docker-compose up -d```
3. Open a browser and hit one of the following endpoints to stream changes

   1. http://localhost:8080/tasks // Subscribes to all changes in all tasks
   2. http://localhost:8080/tasks/$ownerId // e.g. localhost:8080/tasks/1234 - Subscribes to all changes in tasks with specified ownerId
   3. http://localhost:8080/tasks/$ownerId/$id // e.g. localhost:8080/tasks/1234/5678 - Subscribes to all changes in tasks with specified ownerId and id
 
4. Write a task using the following command:
    ```
    curl --location --request POST 'http://127.0.0.1:8080/tasks' \
    --header 'Content-Type: application/json' \
    --data-raw '{
       "id": 1234,
       "ownerId": 5678,
       "detail": "This task is a great task, it will be nice to complete it",
       "done": false
    }'
    ```
5. Update the task using the following command:
   ```
   curl --location --request POST 'http://localhost:8080/tasks' \
   --header 'Content-Type: application/json' \
   --data-raw '{
       "id": 1234,
       "ownerId": 5678,
       "detail": "This task is finally over, it was so bad I don'\''t want to go into detail.",
       "done": true
   }'
   ```



### Run application
`sbt run`

### Run tests    
    docker-compose -f docker-compose-test.yml up -d
    sbt test
    

### Build docker image locally
`sbt docker:publishLocal`

### Create Dockerfile 
`sbt docker:stage` 
