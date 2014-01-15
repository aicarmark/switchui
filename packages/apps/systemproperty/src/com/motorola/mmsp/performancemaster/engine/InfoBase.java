
package com.motorola.mmsp.performancemaster.engine;

import java.util.HashSet;
import java.util.Iterator;

/**
 * abstract class for Information representation
 * 
 * @author dfb746
 */
public abstract class InfoBase {

    /**
     * interface for listening the information update
     * 
     * @author dfb746
     */
    public interface InfoListener {
        public void onInfoUpdate();
    }

    private final HashSet<InfoListener> mListeners = new HashSet<InfoListener>();

    public void registerListener(InfoListener listener) {
        synchronized (this) {

            if (!mListeners.contains(listener)) {
                mListeners.add(listener);

                if (mListeners.size() == 1) {
                    startInfoUpdate();
                }
            }
        }
    }

    public void unregisterListener(InfoListener listener) {
        synchronized (this) {

            if (mListeners.contains(listener)) {
                mListeners.remove(listener);

                if (mListeners.isEmpty()) {
                    stopInfoUpdate();
                }
            }
        }
    }

    /**
     * start updating the information
     */
    protected void startInfoUpdate() {
    }

    /**
     * stop updating the information
     */
    protected void stopInfoUpdate() {
    }

    /**
     * called when there's information update
     */
    protected void onInfoUpdate() {
        synchronized (this) {
            // if (mListener != null) {
            // mListener.onInfoUpdate(this);
            // }

            for (InfoListener listener : mListeners) {
                listener.onInfoUpdate();
            }
        }
    }
}
