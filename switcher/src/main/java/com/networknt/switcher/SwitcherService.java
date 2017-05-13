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

import java.util.List;

/**
 * The interface of switcher service.
 *
 * @author maijunsheng
 * @author zhanglei
 * 
 */
public interface SwitcherService {
    /**
     * Get switcher by name
     * 
     * @param name name of the switcher
     * @return Switcher
     */
    Switcher getSwitcher(String name);

    /**
     * Get all switchers as a List
     * 
     * @return  List
     */
    List<Switcher> getAllSwitchers();

    /**
     * Initiate a value for a switcher
     *
     * @param switcherName switcher name
     * @param initialValue initial value
     */
    void initSwitcher(String switcherName, boolean initialValue);

    /**
     * Check if a switcher is on or off
     * 
     * @param switcherName switcher name
     * @return boolean true ： switcher is on, false：switcher is off
     */
    boolean isOpen(String switcherName);

    /**
     * Check if a switcher is on or off, it doesn't exist, set default value
     * 
     * @param switcherName switcher name
     * @param defaultValue default value
     * @return boolean If switcher exists, return the state, otherwise, set default value and return that value.
     */
    boolean isOpen(String switcherName, boolean defaultValue);

    /**
     * Set switcher state
     * 
     * @param switcherName switcher name
     * @param value value to be set
     */
    void setValue(String switcherName, boolean value);

    /**
     * register a listener for switcher value change, register a listener twice will only fire once
     * 
     * @param switcherName switcher name
     * @param listener listener
     */
    void registerListener(String switcherName, SwitcherListener listener);

    /**
     * unregister a listener
     * 
     * @param switcherName switcher name
     * @param listener the listener to be unregistered, null for all listeners for this switcherName
     */
    void unRegisterListener(String switcherName, SwitcherListener listener);

}
