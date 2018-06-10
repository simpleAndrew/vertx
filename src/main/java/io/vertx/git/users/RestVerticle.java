package io.vertx.git.users;

import io.vertx.core.json.Json;
import io.vertx.git.users.github.GitHubUsersFinder;
import io.vertx.git.users.github.http.ApiLimitReachedException;
import io.vertx.git.users.model.User;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Route;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.client.WebClient;
import io.vertx.rxjava.ext.web.handler.ResponseContentTypeHandler;

import java.util.List;


public class RestVerticle extends AbstractVerticle {

    private static final int API_LIMIT_REACHED_CODE = 403;
    private static final int DEFAULT_STATUS_CODE = 404;

    private static final String USER_PARAM = "user";
    private static final String LANGUAGE_PARAM = "language";
    private static final String SEARCH_PATH = "/search";

    private GitHubUsersFinder gitSearchService;

    @Override
    public void start() {
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);

        WebClient webClient = WebClient.create(vertx);
        gitSearchService = new GitHubUsersFinder(webClient);

        Route searchRoute = router.get(SEARCH_PATH).produces("application/json");
        searchRoute.handler(ResponseContentTypeHandler.create()).handler(this::handleRequest);

        httpServer.requestHandler(router::accept).listen(8080);
    }

    private void handleRequest(RoutingContext context) {
        String user = context.request().getParam(USER_PARAM);
        String language = context.request().getParam(LANGUAGE_PARAM);
        gitSearchService.findUsers(user, language).toList().toSingle()
                .subscribe(
                        json -> context.response().end(convertToJson(json)),
                        throwable -> handleError(context, throwable)
                );
    }

    private void handleError(RoutingContext context, Throwable exception) {
        context.response().putHeader("Content-Type", "text/plain");

        if (ApiLimitReachedException.class.isInstance(exception)) {
            context.response().setStatusCode(API_LIMIT_REACHED_CODE)
                    .end("Github API limit reached. Please, wait for reset");
            return;
        }

        context.response().setStatusCode(DEFAULT_STATUS_CODE).end();
    }

    private static String convertToJson(List<User> json) {
        return Json.encode(json);
    }
}
