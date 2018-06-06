package io.vertx.git.users.http;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.client.HttpRequest;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import rx.Single;

import javax.xml.ws.http.HTTPException;
import java.net.URL;

@RequiredArgsConstructor
public class SafeWebClient {

    private static final int DEFAULT_TIMEOUT_MILLIS = 1000;

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
        HttpRequest<Buffer> request = buildGetRequest()
                .addQueryParam(paramName, paramValue);
        return doSafeRequest(request);

    }

    private HttpRequest<Buffer> buildGetRequest() {
        return client.get(url.getHost(), url.getPath())
                .timeout(timeoutMillis);
    }

    private Single<JsonObject> doSafeRequest(HttpRequest<Buffer> request) {
        return request.rxSend()
                .flatMap(this::leaveOnlySuccess)
                .map(HttpResponse::bodyAsJsonObject);
    }

    private Single<HttpResponse<Buffer>> leaveOnlySuccess(HttpResponse<Buffer> resp) {
        if (resp.statusCode() == 200) {
            return Single.just(resp);
        }
        return Single.error(new HTTPException(resp.statusCode()));
    }

}
