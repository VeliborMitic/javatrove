/*
 * Copyright 2016-2020 Andres Almiray
 *
 * This file is part of Java Trove Examples
 *
 * Java Trove Examples is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Java Trove Examples is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Java Trove Examples. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kordamp.javatrove.example04.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.Data;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kordamp.javatrove.example04.AppConfig;
import org.kordamp.javatrove.example04.model.Repository;
import org.kordamp.javatrove.example04.service.Github;
import org.kordamp.javatrove.example04.service.GithubAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.kordamp.javatrove.example04.TestHelper.createSampleRepositories;
import static org.kordamp.javatrove.example04.TestHelper.repositoriesAsJSON;

/**
 * @author Andres Almiray
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = GithubImplTest.Config.class)
public class GithubImplTest {
    private static final String ORGANIZATION = "foo";

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Inject private ObjectMapper objectMapper;
    @Inject private Github github;

    @Test
    public void happyPath() throws Exception {
        // given:
        String nextUrl = "/organizations/1/repos?page=2";
        List<Repository> repositories = createSampleRepositories();
        stubFor(get(urlEqualTo("/orgs/" + ORGANIZATION + "/repos"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/json")
                .withHeader("Link", "<http://localhost:8080" + nextUrl + ">; rel=\"next\"")
                .withBody(repositoriesAsJSON(repositories.subList(0, 5), objectMapper))));
        stubFor(get(urlEqualTo(nextUrl))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/json")
                .withBody(repositoriesAsJSON(repositories.subList(5, 8), objectMapper))));

        // when:
        List<Repository> results = new ArrayList<>();
        github.repositories(ORGANIZATION)
            .subscribe(results::add);

        // then:
        assertThat(results, hasSize(8));
        verify(getRequestedFor(urlEqualTo("/orgs/" + ORGANIZATION + "/repos")));
        verify(getRequestedFor(urlEqualTo(nextUrl)));
    }

    @Test
    public void failurePath() {
        // given:
        String nextUrl = "/organizations/1/repos?page=2";
        stubFor(get(urlEqualTo("/orgs/" + ORGANIZATION + "/repos"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "text/json")
                .withHeader("Link", "<http://localhost:8080" + nextUrl + ">; rel=\"next\"")
                .withBody(repositoriesAsJSON(createSampleRepositories().subList(0, 5), objectMapper))));
        stubFor(get(urlEqualTo(nextUrl))
            .willReturn(aResponse()
                .withStatus(500)
                .withStatusMessage("Internal Error")));

        // when:
        ThrowableCapture capture = new ThrowableCapture();
        List<Repository> results = new ArrayList<>();
        github.repositories(ORGANIZATION)
            .doOnError(capture::setThrowable)
            .onErrorReturn(throwable -> null)
            .doOnNext(results::add)
            .subscribe();

        // then:
        assertThat(capture.getThrowable(), notNullValue());
        assertThat(capture.getThrowable().getMessage(), equalTo("Internal Error"));
        verify(getRequestedFor(urlEqualTo("/orgs/" + ORGANIZATION + "/repos")));
        verify(getRequestedFor(urlEqualTo(nextUrl)));
    }

    @Data
    private static class ThrowableCapture {
        private Throwable throwable;
    }

    @Configuration
    @ComponentScan("org.kordamp.javatrove.example04.impl")
    static class Config extends AppConfig {
        @Bean
        @Named(GithubAPI.GITHUB_API_URL_KEY)
        public String githubApiUrl() {
            return "http://localhost:8080";
        }
    }
}
