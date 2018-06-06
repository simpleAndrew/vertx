package io.vertx.git.users;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.git.users.http.SafeWebClient;
import io.vertx.rxjava.ext.web.client.WebClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import rx.Observable;
import rx.Single;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@RequiredArgsConstructor
public class GithubUserClient {

    private static final String SEARCH_API_URL = "http://api.github.com/search/users";

    private static URL gitHubUrl;

    static {
        try {
            gitHubUrl = new URL(SEARCH_API_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String QUERY_PARAM = "q";

    private final WebClient client;

    public Observable<JsonObject> searchByNameAndLanguage(@NonNull String username, String language) {
        return new SafeWebClient(client, gitHubUrl)
                .get(QUERY_PARAM, buildQuery(username, language))
                .map(json -> json.getJsonArray("items"))
                .toObservable()
                .map(this::convertIntoJsons)
                .flatMap(Observable::from);
    }

    public Single<JsonObject> getByProfile(@NonNull URL profileUrl) {
        return new SafeWebClient(client, profileUrl).get();
    }

    private List<JsonObject> convertIntoJsons(JsonArray jsonArray) {
        ArrayList<JsonObject> result = new ArrayList<>(jsonArray.size());
        for (int i = 0; i < jsonArray.size(); i++) {
            result.add(jsonArray.getJsonObject(i));
        }
        return result;
    }

    @SneakyThrows
    private String buildQuery(@NonNull String username, String language) {
        String userQuery = username + "+type:user+in:login";
        String finalQuery = userQuery + Optional.ofNullable(language)
                .map(lang -> "+language:" + lang)
                .orElse("");
        return URLDecoder.decode(finalQuery, "UTF-8");
    }
}
