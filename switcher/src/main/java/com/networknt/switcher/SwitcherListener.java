package com.networknt.switcher;

/**
 * A listener interface to get notified if switcher state is changed.
 *
 * @author axb
 */
public interface SwitcherListener {

    void onValueChanged(String key,Boolean value);
}
