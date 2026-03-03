/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.networknt.switcher;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * LocalSwitcherService test case.
 *
 * @author maijunsheng
 *
 */
public class LocalSwitcherServiceTest {

    @Test
    public void testProtocolSwitcher() {

        String protocolSwitcher = "HeartBeatSwitcher";

        LocalSwitcherService localSwitcherService = new LocalSwitcherService();
        localSwitcherService.setValue(protocolSwitcher, false);

        Switcher switcher = localSwitcherService.getSwitcher(protocolSwitcher);

        Assertions.assertNotNull(switcher);
        Assertions.assertFalse(switcher.isOn());

        localSwitcherService.setValue(protocolSwitcher, true);

        switcher = localSwitcherService.getSwitcher(protocolSwitcher);
        Assertions.assertNotNull(switcher);
        Assertions.assertTrue(switcher.isOn());

    }
}
