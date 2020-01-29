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
package org.kordamp.javatrove.example05;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import javafx.scene.control.Button;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.kordamp.javatrove.example05.impl.GithubImpl;
import org.kordamp.javatrove.example05.model.Repository;
import org.kordamp.javatrove.example05.service.Github;
import org.kordamp.javatrove.example05.service.GithubAPI;
import org.kordamp.javatrove.example05.util.ApplicationEventHandler;
import org.kordamp.javatrove.example05.view.AppView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testfx.framework.junit.ApplicationRule;
import org.testfx.matcher.base.WindowMatchers;
import org.testfx.service.support.WaitUntilSupport;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.kordamp.javatrove.example05.TestHelper.createSampleRepositories;
import static org.kordamp.javatrove.example05.TestHelper.repositoriesAsJSON;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isEnabled;
import static org.testfx.matcher.control.LabeledMatchers.hasText;
import static org.testfx.matcher.control.ListViewMatchers.hasItems;

/**
 * @author Andres Almiray
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = FunctionalTest.Config.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FunctionalTest {
    private static final String ORGANIZATION = "foo";

    @Inject private ObjectMapper objectMapper;
    @Inject private AppView view;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8080);

    @Rule
    public ApplicationRule testfx = new ApplicationRule(stage -> {
        stage.setScene(view.createScene());
        stage.sizeToScene();
        stage.setResizable(false);
        stage.show();
    });

    @Test
    public void _01_happy_path() throws Exception {
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
                .withBody(repositoriesAsJSON(repositories.subList(5, 10), objectMapper))));

        // when:
        testfx.clickOn("#organization")
            .eraseText(ORGANIZATION.length())
            .write(ORGANIZATION);
        testfx.clickOn("#loadButton");

        // wait
        Button loadButton = testfx.lookup("#loadButton").query();
        new WaitUntilSupport().waitUntil(loadButton, isEnabled(), 2);

        // then:
        verifyThat("#total", hasText("10"));
        verifyThat("#repositories", hasItems(10));
    }

    @Test
    public void _02_cancel_path() throws Exception {
        // given:
        String nextUrl = "/organizations/1/repos?page=2";
        List<Repository> repositories = createSampleRepositories();
        stubFor(get(urlEqualTo("/orgs/" + ORGANIZATION + "/repos"))
            .willReturn(aResponse()
                .withFixedDelay(200)
                .withStatus(200)
                .withHeader("Content-Type", "text/json")
                .withHeader("Link", "<http://localhost:8080" + nextUrl + ">; rel=\"next\"")
                .withBody(repositoriesAsJSON(repositories.subList(0, 5), objectMapper))));
        stubFor(get(urlEqualTo(nextUrl))
            .willReturn(aResponse()
                .withFixedDelay(200)
                .withStatus(200)
                .withHeader("Content-Type", "text/json")
                .withBody(repositoriesAsJSON(repositories.subList(5, 10), objectMapper))));

        // when:
        testfx.clickOn("#organization")
            .eraseText(ORGANIZATION.length())
            .write(ORGANIZATION);
        testfx.clickOn("#loadButton");
        testfx.clickOn("#cancelButton");

        // wait
        Button loadButton = testfx.lookup("#loadButton").query();
        new WaitUntilSupport().waitUntil(loadButton, isEnabled(), 2);

        // then:
        verifyThat("#total", hasText("5"));
        verifyThat("#repositories", hasItems(5));
    }

    @Test
    public void _03_failure_path() {
        // given:
        stubFor(get(urlEqualTo("/orgs/" + ORGANIZATION + "/repos"))
            .willReturn(aResponse()
                .withStatus(500)
                .withStatusMessage("Internal Error")));

        // when:
        testfx.clickOn("#organization")
            .eraseText(ORGANIZATION.length())
            .write(ORGANIZATION);
        testfx.clickOn("#loadButton");

        // then:
        new WaitUntilSupport().waitUntil(testfx.window("Error"), WindowMatchers.isShowing(), 5);
    }

    @Configuration
    @ComponentScan("org.kordamp.javatrove.example05")
    static class Config extends AppConfig {
        @Bean
        @Named(GithubAPI.GITHUB_API_URL_KEY)
        public String githubApiUrl() {
            return "http://localhost:8080";
        }

        @Bean
        public Github github() {
            return new GithubImpl();
        }

        @Bean
        public ApplicationEventHandler applicationEventHandler() {
            return new ApplicationEventHandler();
        }
    }
}
