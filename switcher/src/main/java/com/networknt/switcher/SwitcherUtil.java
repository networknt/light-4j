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


/**
 * A static switcher utility. Normally it is used as global switcher.
 * 
 * @author zhanglei
 *
 */
public class SwitcherUtil {
    private static SwitcherService switcherService = new LocalSwitcherService();

    public static void initSwitcher(String switcherName, boolean initialValue) {
        switcherService.initSwitcher(switcherName, initialValue);
    }

    /**
     * Check if switcher is on or off
     * 
     * @param switcherName switcher name
     * @return boolean true ：switcher is on and value is true, false：switcher is off and value is false
     */
    public static boolean isOpen(String switcherName) {
        return switcherService.isOpen(switcherName);
    }

    /**
     * Check if switcher is on or off. If it doesn't exist, set default value and return the default value
     * 
     * @param switcherName switcher name
     * @param defaultValue default value
     * @return boolean If switcher exists, return the state, otherwise, set default value and return it.
     */
    public static boolean switcherIsOpenWithDefault(String switcherName, boolean defaultValue) {
        return switcherService.isOpen(switcherName, defaultValue);
    }

    /**
     * Set switcher state
     * 
     * @param switcherName switcher name
     * @param value value to be set
     */
    public static void setSwitcherValue(String switcherName, boolean value) {
        switcherService.setValue(switcherName, value);
    }

    public static SwitcherService getSwitcherService() {
        return switcherService;
    }

    public static void setSwitcherService(SwitcherService switcherService) {
        SwitcherUtil.switcherService = switcherService;
    }

    public static void registerSwitcherListener(String switcherName, SwitcherListener listener) {
        switcherService.registerListener(switcherName, listener);
    }

}
