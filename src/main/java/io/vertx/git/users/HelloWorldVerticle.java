package io.vertx.git.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.Json;
import io.vertx.git.users.model.User;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServerRequest;
import io.vertx.rxjava.ext.web.client.WebClient;

import java.util.List;


public class HelloWorldVerticle extends AbstractVerticle {

    private RequestHandler gitSearchService;

    @Override
    public void start() {

        WebClient webClient = WebClient.create(vertx);
        gitSearchService = new RequestHandler(webClient);
        ObjectMapper mapper = new ObjectMapper();
        vertx.createHttpServer()
                .requestHandler(this::getSimpleAndrew)
                .listen(8080);
    }

    private void getSimpleAndrew(HttpServerRequest req) {
        String user = req.getParam("user");
        String language = req.getParam("lang");
        gitSearchService.findUsers(user, language)
                .subscribe(
                        json -> req.response()
                                .putHeader("Content-Type", "application/json")
                                .end(convertResponse(json)),
                        throwable -> {
                            System.out.println("throwable = " + throwable);
                            throwable.printStackTrace();
                            req.response().setStatusCode(404).end();
                        }
                );
    }

    private String convertResponse(List<User> json) {
        return Json.encode(json);
    }

}
