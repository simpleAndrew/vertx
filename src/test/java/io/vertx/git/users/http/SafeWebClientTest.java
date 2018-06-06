package io.vertx.git.users.http;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.ext.web.client.HttpRequest;
import io.vertx.rxjava.ext.web.client.HttpResponse;
import io.vertx.rxjava.ext.web.client.WebClient;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Single;

import javax.xml.ws.http.HTTPException;
import java.net.URL;

import static org.assertj.core.api.Fail.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SafeWebClientTest {

    @Mock
    private WebClient client;

    @Mock
    private HttpRequest<Buffer> request;

    @Before
    public void mockClientBehaviour() {
        when(client.get(any(), any())).thenReturn(request);
        when(request.addQueryParam(anyString(), anyString())).thenReturn(request);
        when(request.timeout(anyLong())).thenReturn(request);

        when(request.rxSend()).thenReturn(Single.error(new NullPointerException()));
    }

    @Test
    public void shouldCallCorrectAPI() {
        //given
        val host = "some.com";
        val path = "/any";
        val expectedUrl = getUrl("http://" + host + path);

        //when
        buildClient(expectedUrl).get();

        //then
        verify(client).get(eq(host), eq(path));
    }

    @Test
    public void shouldSetTimeout() {
        //give
        buildClient().get();

        //then
        verify(request).timeout(anyLong());
    }

    @Test
    public void shouldPassQuery() {
        //given
        val expectedQueryParam = "q";
        val expectedValue = "val";

        //when
        buildClient().get(expectedQueryParam, expectedValue);

        //then
        verify(request).addQueryParam(eq(expectedQueryParam), eq(expectedValue));
    }
    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfParamNameIsNull() {
        //when
        buildClient().get(null, "any");

        //then
        fail("Should fail when paramName is null");
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfParamValueIsNull() {
        //when
        buildClient().get("any", null);

        //then
        fail("Should fail when paramValue is null");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldTreatNon200AsErrors() {
        //given
        HttpResponse<Buffer> mockedResponse = Mockito.mock(HttpResponse.class);
        when(mockedResponse.statusCode()).thenReturn(500);
        when(request.rxSend()).thenReturn(Single.just(mockedResponse));

        //when
        Single<JsonObject> errorResponse = buildClient().get();

        //then
        errorResponse.test().assertError(HTTPException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldMapResponseToJson() {
        //given
        val expectedJson = new JsonObject();

        HttpResponse<Buffer> mockedResponse = Mockito.mock(HttpResponse.class);
        when(mockedResponse.statusCode()).thenReturn(200);
        when(mockedResponse.bodyAsJsonObject()).thenReturn(expectedJson);
        when(request.rxSend()).thenReturn(Single.just(mockedResponse));

        //when
        Single<JsonObject> result = buildClient().get();

        //then
        result.test().assertResult(expectedJson);
    }

    private SafeWebClient buildClient() {
        return buildClient(getUrl("http://github.com"));
    }

    private SafeWebClient buildClient(URL expectedUrl) {
        return new SafeWebClient(client, expectedUrl);
    }

    @SneakyThrows
    private URL getUrl(String spec) {
        return new URL(spec);
    }


}