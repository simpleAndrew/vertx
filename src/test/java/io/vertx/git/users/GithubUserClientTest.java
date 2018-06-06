package io.vertx.git.users;

import io.vertx.core.json.JsonArray;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;
import rx.Single;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Fail.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GithubUserClientTest {

    @Mock
    private WebClient client;

    @Mock
    private HttpRequest<Buffer> request;

    @InjectMocks
    private GithubUserClient searchClient;

    @Before
    public void mockClientBehaviour() {
        when(client.get(any(), any())).thenReturn(request);
        when(request.addQueryParam(anyString(), anyString())).thenReturn(request);
        when(request.timeout(anyLong())).thenReturn(request);

        when(request.rxSend()).thenReturn(Single.error(new NullPointerException()));
    }

    @Test
    public void shouldSearchForUsersOnly() {
        //when
        searchClient.searchByNameAndLanguage("any", "any");

        //then
        verify(request).addQueryParam(any(), Mockito.contains("type:user"));
    }

    @Test
    public void shouldSearchByLogin() {
        //when
        searchClient.searchByNameAndLanguage("any", "any");

        //then
        verify(request).addQueryParam(any(), Mockito.contains("in:login"));
    }

    @Test
    public void shouldIncludePassedUserNameInQuery() {
        //given
        val expectedUsername = "any";

        //when
        searchClient.searchByNameAndLanguage(expectedUsername, "any");

        //then
        verify(request).addQueryParam(any(), Mockito.contains(expectedUsername));
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionIfUsernameIsNull() {
        //when
        searchClient.searchByNameAndLanguage(null, "any");

        //then
        fail("Should fail when username is null");
    }

    @Test
    public void shouldIncludeLanguage() {
        //given
        val expectedLanguage = "delphi";
        val expectedQuery = "language:" + expectedLanguage;

        //when
        searchClient.searchByNameAndLanguage("any", expectedLanguage);

        //then
        verify(request).addQueryParam(any(), Mockito.contains(expectedQuery));
    }

    @Test
    public void shouldContinueOnNullLanguage() {
        //when
        searchClient.searchByNameAndLanguage("any", null);

        //then
        verify(request).addQueryParam(any(), any());
    }

    @Test
    @SneakyThrows
    public void shouldRetrieveUserByProfile() {
        //given
        val expectedPath = "/users/simpleAndrew";
        val expectedHost = "api.github.com";
        val url = new URL("http://" + expectedHost + expectedPath);

        //when
        searchClient.getByProfile(url);

        //then
        verify(client).get(eq(expectedHost), eq(expectedPath));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnSearchItems() {
        //given
        val recordsCount = 10;
        val expectedJsons = getJsonObjects(recordsCount);
        val resultingJson = buildSearchResposne(expectedJsons);
        HttpResponse<Buffer> mockedResponse = Mockito.mock(HttpResponse.class);
        when(mockedResponse.statusCode()).thenReturn(200);
        when(mockedResponse.bodyAsJsonObject()).thenReturn(resultingJson);
        when(request.rxSend()).thenReturn(Single.just(mockedResponse));

        //when
        Observable<JsonObject> result = searchClient.searchByNameAndLanguage("any", "any");

        //then
        result.test().assertValues(expectedJsons.toArray(new JsonObject[recordsCount]));
    }

    private List<JsonObject> getJsonObjects(int itemsCount) {
        return IntStream.range(0, itemsCount)
                    .mapToObj(index -> new JsonObject().put("index", index))
                    .collect(Collectors.toList());
    }

    private JsonObject buildSearchResposne(List<JsonObject> jsons) {
        return new JsonObject().put("items", new JsonArray(jsons));
    }
}