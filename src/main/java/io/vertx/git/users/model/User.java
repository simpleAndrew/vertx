package io.vertx.git.users.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URL;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    @JsonProperty("login")
    private String login;

    @JsonProperty("name")
    private String name;

    @JsonProperty("followers")
    private long followers;

    @JsonAlias("avatar_url")
    @JsonProperty("avatar")
    private URL avatarUrl;

    @JsonAlias("html_url")
    @JsonProperty("profile")
    private URL profileUrl;
}
