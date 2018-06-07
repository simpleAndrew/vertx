package io.vertx.git.users.github;


import io.vertx.core.json.JsonObject;
import io.vertx.git.users.model.User;
import io.vertx.rxjava.ext.web.client.WebClient;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Single;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
public class GitHubUsersFinder {

    private final GithubUserWebClient githubClient;

    public GitHubUsersFinder(WebClient client) {
        githubClient = new GithubUserWebClient(client);
    }

    @SneakyThrows
    public Single<List<User>> findUsers(@NonNull String userName, String language) {
        log.debug("Search for users with login:{} and language:{}", userName, language);
        return githubClient.searchByNameAndLanguage(userName, language)
                .switchIfEmpty(doFallBackSearch(userName))
                .onErrorResumeNext(t -> tryToFallback(t, userName))
                .flatMap(this::retrieveUser)
                .toList()
                .doOnNext(users -> log.debug("Found {} users for username:{}, language:{}", users.size(), userName, language))
                .toSingle();
    }

    private Observable<JsonObject> tryToFallback(Throwable exception, String userName) {
        if (TimeoutException.class.isInstance(exception)) {
            log.info("Falling back to because of request timeout");
            return doFallBackSearch(userName);
        }
        return Observable.error(exception);
    }

    private Observable<JsonObject> doFallBackSearch(@NonNull String userName) {
        log.info("Falling back on search by ony username: {}", userName);
        return githubClient.searchByNameAndLanguage(userName, null);
    }

    private Observable<User> retrieveUser(JsonObject userSearchResult) {
        return buildUrl(userSearchResult.getString("url"))
                .doOnNext(url -> log.debug("Requesting user information by profile URL:{}", url))
                .flatMapSingle(githubClient::getByProfile)
                .map(json -> json.mapTo(User.class));
    }

    private Observable<URL> buildUrl(String url) {
        try {
            return Observable.just(new URL(url));
        } catch (MalformedURLException e) {
            log.warn("Skipping malformed URL:{}", url, e);
            return Observable.empty();
        }
    }
}
