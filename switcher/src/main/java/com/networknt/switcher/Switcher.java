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
 * Switcher class has a name and an on/off switch
 *
 * @author maijunsheng
 *
 */
public class Switcher {
    private boolean on = true;
    private String name; // name of the switch

    public Switcher(String name, boolean on) {
        this.name = name;
        this.on = on;
    }

    public String getName() {
        return name;
    }

    /**
     * isOn: true，service available; isOn: false, service un-available
     *
     * @return boolean
     */
    public boolean isOn() {
        return on;
    }

    /**
     * turn on switcher
     */
    public void onSwitcher() {
        this.on = true;
    }

    /**
     * turn off switcher
     */
    public void offSwitcher() {
        this.on = false;
    }
}
