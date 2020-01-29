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
package org.kordamp.javatrove.chat03.server.impl;

import org.kordamp.javatrove.chat03.Command;

import static org.kordamp.javatrove.chat03.Command.Type.MESSAGE;

/**
 * @author Andres Almiray
 */
public class ServerMessageCommandHandler extends AbstractServerCommandHandler {
    public static final String NAME = "_MESSAGE_";

    @Override
    public boolean supports(Command.Type commandType) {
        return commandType == MESSAGE;
    }
}
