package io.vertx.git.users;


import io.vertx.core.json.JsonObject;
import io.vertx.git.users.model.User;
import io.vertx.rxjava.ext.web.client.WebClient;
import lombok.NonNull;
import lombok.SneakyThrows;
import rx.Observable;
import rx.Single;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class RequestHandler {

    private final GithubUserClient githubClient;

    public RequestHandler(WebClient client) {
        githubClient = new GithubUserClient(client);
    }

    @SneakyThrows
    Single<List<User>> findUsers(@NonNull String userName, String language) {
        return githubClient.searchByNameAndLanguage(userName, language)
                .switchIfEmpty(doFallBackSearch(userName))
                .onErrorResumeNext(t -> tryToFallback(t, userName))
                .flatMap(this::retrieveUser)
                .toList().toSingle();
    }

    private Observable<JsonObject> tryToFallback(Throwable exception, String userName) {
        if (TimeoutException.class.isInstance(exception)) {
            return doFallBackSearch(userName);
        }
        return Observable.error(exception);
    }

    private Observable<JsonObject> doFallBackSearch(@NonNull String userName) {
        return githubClient.searchByNameAndLanguage(userName, null);
    }

    private Observable<User> retrieveUser(JsonObject userSearchResult) {
        return buildUrl(userSearchResult.getString("url"))
                .flatMapSingle(githubClient::getByProfile)
                .map(json -> json.mapTo(User.class));
    }

    private Observable<URL> buildUrl(String url) {
        try {
            return Observable.just(new URL(url));
        } catch (MalformedURLException e) {
            return Observable.empty();
        }
    }
}
