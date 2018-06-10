# Simple Git user search app

Small implementation to use git search api for users.
Allows to search by username and language.

Uses unauthorised API, so API call restrictions may apply.

# Pre run setup

Make sure you have docker with logged user (will be needed to download base image) 

# How to build the docker image 

Execute command

```
mvnw clean package docker:build
```

Use mvnw.cmd on windows

As a result you'll have an image generated. 
Application image is called *vertx/git-users-api*.


# Quick run

Execute command
```
mvnw docker:start
```
It will start application on port 8080.

# How to use

Application starts application with HTTP server that listens for GET requests on URI "/search" with params "user" and "language" on port 8080.

To fulfill request application executes search and then requests profiles of each returned user to get number of followers.
If first search fails (timeouts or finds no profiles), then app tries to search again with only username.

Result of search looks like:
```
[
    {
        "login": "simpleAndrew",
        "name": "Andrew Shchyolok",
        "followers": 1,
        "avatar": "https://avatars3.githubusercontent.com/u/1022228?v=4",
        "profile": "https://github.com/simpleAndrew"
    }
]
```

Note: application uses unauthorised Github API, which has some request limits (typically - 10 search requests per minute and 60 other request per 2 hours per IP)
 
# Examples
To find me
```
curl localhost:8080/serch?user=simpleAndrew
```
To find people who love pascal
```
curl localhost:8080/serch?user=simple&language=pascal

```
