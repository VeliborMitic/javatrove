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
package org.kordamp.javatrove.example04.view;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.util.converter.NumberStringConverter;
import org.kordamp.javatrove.example04.controller.AppController;
import org.kordamp.javatrove.example04.model.AppModel;
import org.kordamp.javatrove.example04.model.Repository;
import org.reactfx.EventStreams;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;

import static java.util.Objects.requireNonNull;
import static javafx.beans.binding.Bindings.bindBidirectional;
import static org.kordamp.javatrove.example04.model.State.DISABLED;
import static org.kordamp.javatrove.example04.model.State.READY;
import static org.kordamp.javatrove.example04.util.StringUtils.isBlank;

/**
 * @author Andres Almiray
 */
@Component
public class AppView {
    @Inject private AppController controller;
    @Inject private AppModel model;

    @FXML private TextField organization;
    @FXML private TextField limit;
    @FXML private Button loadButton;
    @FXML private Button cancelButton;
    @FXML private ProgressBar progress;
    @FXML private Label total;
    @FXML private ListView<Repository> repositories;

    private BooleanProperty enabled = new SimpleBooleanProperty(this, "enabled", true);
    private BooleanProperty running = new SimpleBooleanProperty(this, "running", false);

    public Scene createScene() {
        String basename = getClass().getPackage().getName().replace('.', '/') + "/app";
        URL fxml = getClass().getClassLoader().getResource(basename + ".fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(fxml);
        fxmlLoader.setControllerFactory(param -> AppView.this);
        Parent root = null;
        try {
            root = (Parent) fxmlLoader.load();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        organization.textProperty().addListener((observable, oldValue, newValue) -> {
            model.setState(isBlank(newValue) ? DISABLED : READY);
        });

        model.stateProperty().addListener((observable, oldValue, newValue) ->
            Platform.runLater(() -> {
                switch (newValue) {
                    case DISABLED:
                        enabled.setValue(false);
                        running.setValue(false);
                        break;
                    case READY:
                        enabled.setValue(true);
                        running.setValue(false);
                        break;
                    case RUNNING:
                        enabled.setValue(false);
                        running.setValue(true);
                        break;
                }
            }));

        ObservableList<Repository> items = createJavaFXThreadProxyList(model.getRepositories().sorted());

        repositories.setItems(items);
        EventStreams.sizeOf(items).subscribe(v -> total.setText(String.valueOf(v)));
        organization.textProperty().bindBidirectional(model.organizationProperty());
        bindBidirectional(limit.textProperty(), model.limitProperty(), new NumberStringConverter());
        loadButton.disableProperty().bind(Bindings.not(enabled));
        cancelButton.disableProperty().bind(Bindings.not(running));
        progress.visibleProperty().bind(running);

        Scene scene = new Scene(root);
        scene.getStylesheets().addAll(basename + ".css", "org/kordamp/bootstrapfx/bootstrapfx.css");
        return scene;
    }

    public void load(ActionEvent ignored) {
        controller.load();
    }

    public void cancel(ActionEvent ignored) {
        controller.cancel();
    }

    private static <E> ObservableList<E> createJavaFXThreadProxyList(ObservableList<E> source) {
        requireNonNull(source, "Argument 'source' must not be null");
        return new JavaFXThreadProxyObservableList<>(source);
    }

    private static class JavaFXThreadProxyObservableList<E> extends TransformationList<E, E> {
        protected JavaFXThreadProxyObservableList(ObservableList<E> source) {
            super(source);
        }

        @Override
        protected void sourceChanged(ListChangeListener.Change<? extends E> c) {
            if (Platform.isFxApplicationThread()) {
                fireChange(c);
            } else {
                Platform.runLater(() -> fireChange(c));
            }
        }

        @Override
        public int getSourceIndex(int index) {
            return index;
        }

        @Override
        public E get(int index) {
            return getSource().get(index);
        }

        @Override
        public int size() {
            return getSource().size();
        }

        @Override
        public int getViewIndex(int index) {
            return index;
        }
    }
}
