package io.vertx.git.users.github;


import io.vertx.core.json.JsonObject;
import io.vertx.git.users.model.User;
import io.vertx.rxjava.ext.web.client.WebClient;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeoutException;

@Slf4j
public class GitHubUsersFinder {

    private static final String PROFILE_URL_FIELD = "url";

    private final GithubUserWebClient githubClient;

    public GitHubUsersFinder(WebClient client) {
        githubClient = new GithubUserWebClient(client);
    }

    @SneakyThrows
    public Observable<User> findUsers(String userName, String language) {
        if(userName == null || userName.isEmpty()) {
            return Observable.error(new IllegalArgumentException("userName should be provided"));
        }

        log.debug("Search for users with login:{} and language:{}", userName, language);

        Observable<JsonObject> searchObservable = language != null
                ? findWithLanguage(userName, language)
                : findWithoutLanguage(userName);

        return searchObservable.flatMap(this::getUserFromProfile);
    }

    private Observable<JsonObject> findWithLanguage(@NonNull String userName, String language) {
        return githubClient.searchByNameAndLanguage(userName, language)
                .onErrorResumeNext(this::treatTimeoutAsEmpty)
                .switchIfEmpty(findWithoutLanguage(userName));
    }

    private Observable<JsonObject> treatTimeoutAsEmpty(Throwable exception) {
        if (TimeoutException.class.isInstance(exception)) {
            log.info("Request timed out - treating it as empty");
            return Observable.empty();
        }
        return Observable.error(exception);
    }

    private Observable<JsonObject> findWithoutLanguage(@NonNull String userName) {
        return githubClient.searchByNameAndLanguage(userName, null)
                .doOnSubscribe(() -> log.debug("Search users by username only: {}", userName));
    }

    private Observable<User> getUserFromProfile(JsonObject userSearchResult) {
        return buildUrl(userSearchResult.getString(PROFILE_URL_FIELD))
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
