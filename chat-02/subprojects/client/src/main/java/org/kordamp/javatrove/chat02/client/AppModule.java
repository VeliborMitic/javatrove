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
package org.kordamp.javatrove.chat02.client;

import org.jdeferred.DeferredManager;
import org.kordamp.javatrove.chat02.client.controller.AppController;
import org.kordamp.javatrove.chat02.client.impl.ChatClientImpl;
import org.kordamp.javatrove.chat02.client.impl.ClientCommandDispatcherImpl;
import org.kordamp.javatrove.chat02.client.impl.ClientLoginCommandHandler;
import org.kordamp.javatrove.chat02.client.impl.ClientLogoutCommandHandler;
import org.kordamp.javatrove.chat02.client.impl.ClientMessageCommandHandler;
import org.kordamp.javatrove.chat02.client.impl.DeferredManagerProvider;
import org.kordamp.javatrove.chat02.client.model.AppModel;
import org.kordamp.javatrove.chat02.client.util.ApplicationEventBus;
import org.kordamp.javatrove.chat02.client.util.ApplicationEventHandler;
import org.kordamp.javatrove.chat02.client.view.AppView;
import ru.vyarus.guice.ext.ExtAnnotationsModule;

import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.inject.name.Names.named;

/**
 * @author Andres Almiray
 */
public class AppModule extends ExtAnnotationsModule {
    public AppModule() {
        super(AppModule.class.getPackage().getName());
    }

    @Override
    protected final void configure() {
        super.configure();
        bindExecutorService();
        bindChatClient();
        bindCommandDispatcher();
        bindLoginCommandHandler();
        bindLogoutCommandHandler();
        bindMessageCommandHandler();
        bindDeferredManager();
        bindApplicationEventBus();
        bindApplicationEventHandler();
        bindAppController();
        bindAppModel();
        bindAppView();
    }

    protected void bindExecutorService() {
        bind(ExecutorService.class)
            .toInstance(Executors.newFixedThreadPool(1));
    }

    protected void bindChatClient() {
        bind(ChatClient.class)
            .to(ChatClientImpl.class);
    }

    protected void bindCommandDispatcher() {
        bind(ClientCommandDispatcher.class)
            .to(ClientCommandDispatcherImpl.class)
            .in(Singleton.class);
    }

    protected void bindLoginCommandHandler() {
        bind(ClientCommandHandler.class)
            .annotatedWith(named(ClientLoginCommandHandler.NAME))
            .to(ClientLoginCommandHandler.class)
            .in(Singleton.class);
    }

    protected void bindLogoutCommandHandler() {
        bind(ClientCommandHandler.class)
            .annotatedWith(named(ClientLogoutCommandHandler.NAME))
            .to(ClientLogoutCommandHandler.class)
            .in(Singleton.class);
    }

    protected void bindMessageCommandHandler() {
        bind(ClientCommandHandler.class)
            .annotatedWith(named(ClientMessageCommandHandler.NAME))
            .to(ClientMessageCommandHandler.class)
            .in(Singleton.class);
    }

    protected void bindDeferredManager() {
        bind(DeferredManager.class)
            .toProvider(DeferredManagerProvider.class)
            .in(Singleton.class);
    }

    protected void bindApplicationEventBus() {
        bind(ApplicationEventBus.class)
            .in(Singleton.class);
    }

    protected void bindApplicationEventHandler() {
        bind(ApplicationEventHandler.class)
            .asEagerSingleton();
    }

    protected void bindAppController() {
        bind(AppController.class).in(Singleton.class);
    }

    protected void bindAppModel() {
        bind(AppModel.class).in(Singleton.class);
    }

    protected void bindAppView() {
        bind(AppView.class).in(Singleton.class);
    }
}
