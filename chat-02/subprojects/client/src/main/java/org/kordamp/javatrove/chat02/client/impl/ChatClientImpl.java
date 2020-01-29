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
package org.kordamp.javatrove.chat02.client.impl;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.kordamp.javatrove.chat02.Command;
import org.kordamp.javatrove.chat02.client.ChatClient;
import org.kordamp.javatrove.chat02.client.ClientCommandDispatcher;
import org.kordamp.javatrove.chat02.client.util.ApplicationEventBus;
import org.kordamp.javatrove.chat02.client.util.DisconnectEvent;
import org.kordamp.javatrove.chat02.client.util.ThrowableEvent;
import org.kordamp.javatrove.chat02.grpc.ChatGrpc;
import org.kordamp.javatrove.chat02.grpc.Login;
import org.kordamp.javatrove.chat02.grpc.Logout;
import org.kordamp.javatrove.chat02.grpc.Message;
import org.kordamp.javatrove.chat02.grpc.Response;

import javax.inject.Inject;
import java.net.InetAddress;

import static org.kordamp.javatrove.chat02.ChatUtil.NAME_SEPARATOR;
import static org.kordamp.javatrove.chat02.ChatUtil.loginCommand;
import static org.kordamp.javatrove.chat02.ChatUtil.logoutCommand;
import static org.kordamp.javatrove.chat02.ChatUtil.messageCommand;
import static org.kordamp.javatrove.chat02.ChatUtil.toSHA1;

/**
 * @author Andres Almiray
 */
public class ChatClientImpl implements ChatClient {
    @Inject private ClientCommandDispatcher clientDispatcher;
    @Inject private ApplicationEventBus eventBus;

    private String id;
    private ManagedChannel channel;
    private ChatGrpc.ChatBlockingStub blockingStub;
    private ChatGrpc.ChatStub asyncStub;

    @Override
    public void login(String host, int port, String name) {
        channel = ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext(true)
            .build();

        blockingStub = ChatGrpc.newBlockingStub(channel);
        asyncStub = ChatGrpc.newStub(channel);

        id = toSHA1(InetAddress.getLoopbackAddress().getHostName() + "-" + System.nanoTime());
        asyncStub.login(Login.newBuilder()
                .setName(name)
                .setId(id)
                .build(),
            new StreamObserverAdapter<Response>() {
                @Override
                public void onNext(Response value) {
                    clientDispatcher.dispatch(asCommand(value));
                }

                @Override
                public void onError(Throwable throwable) {
                    eventBus.publishAsync(new ThrowableEvent(throwable));
                }
            });
    }

    @Override
    public void logout(String name) {
        blockingStub.logout(Logout.newBuilder()
            .setName(name)
            .setId(id)
            .build());
        channel.shutdownNow();
    }

    @Override
    public void send(String name, String message) {
        asyncStub.send(Message.newBuilder()
            .setMessage(name + NAME_SEPARATOR + " " + message)
            .build(), new StreamObserverAdapter<Empty>() {
            @Override
            public void onError(Throwable throwable) {
                eventBus.publishAsync(new ThrowableEvent(throwable));
                eventBus.publishAsync(new DisconnectEvent(throwable));
            }
        });
    }

    private Command asCommand(Response value) {
        switch (value.getType()) {
            case LOGIN:
                return loginCommand(value.getPayload());
            case LOGOUT:
                return logoutCommand(value.getPayload());
            case MESSAGE:
                return messageCommand(value.getPayload());
        }
        return null;
    }

    private class StreamObserverAdapter<T> implements StreamObserver<T> {
        @Override
        public void onNext(T value) {

        }

        @Override
        public void onError(Throwable t) {

        }

        @Override
        public void onCompleted() {

        }
    }
}
