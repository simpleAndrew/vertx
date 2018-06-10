package io.vertx.git.users.github.http;

public class ApiLimitReachedException extends RuntimeException {

    public ApiLimitReachedException(String allowedLimit) {
        super("Github API limit reached. Allowed number of calls: " + allowedLimit);
    }
}
