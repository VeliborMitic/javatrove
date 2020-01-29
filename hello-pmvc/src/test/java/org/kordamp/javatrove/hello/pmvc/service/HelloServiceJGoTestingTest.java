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
package org.kordamp.javatrove.hello.pmvc.service;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.jgotesting.rule.JGoTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author Andres Almiray
 */
@RunWith(JUnitParamsRunner.class)
public class HelloServiceJGoTestingTest {
    @Rule
    public final JGoTestRule test = new JGoTestRule();

    @Test
    @Parameters(source = HelloParameters.class)
    public void sayHello(String input, String output) {
        // given:
        HelloService service = new DefaultHelloService();
        // expect:
        test.check(service.sayHello(input), equalTo(output));
    }
}
