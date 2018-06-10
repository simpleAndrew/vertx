package io.vertx.git.users.github.http;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.client.HttpRequest;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Single;

import javax.xml.ws.http.HTTPException;
import java.net.URL;

@Slf4j
@RequiredArgsConstructor
public class SafeWebClient {

    private static final int DEFAULT_TIMEOUT_MILLIS = 5000;

    private final WebClient client;
    private final URL url;
    private final long timeoutMillis;

    public SafeWebClient(WebClient client, URL url) {
        this(client, url, DEFAULT_TIMEOUT_MILLIS);
    }

    public Single<JsonObject> get() {
        return doSafeRequest(buildGetRequest());
    }

    public Single<JsonObject> get(@NonNull String paramName, @NonNull String paramValue) {
        return doSafeRequest(buildGetRequest().addQueryParam(paramName, paramValue));
    }

    private HttpRequest<Buffer> buildGetRequest() {
        return client.get(url.getHost(), url.getPath()).timeout(timeoutMillis);
    }

    private Single<JsonObject> doSafeRequest(HttpRequest<Buffer> request) {
        return request.rxSend()
                .flatMap(this::leaveOnlySuccess)
                .map(HttpResponse::bodyAsJsonObject);
    }

    private Single<HttpResponse<Buffer>> leaveOnlySuccess(HttpResponse<Buffer> resp) {
        if (resp.statusCode() == 200) {
            log.debug("Response successful. Body: {}", resp.bodyAsString());
            return Single.just(resp);
        }
        log.warn("Returning error for response with status code: {}. Body: {}", resp.statusCode(), resp.bodyAsString());
        return Single.error(new HTTPException(resp.statusCode()));
    }

}
