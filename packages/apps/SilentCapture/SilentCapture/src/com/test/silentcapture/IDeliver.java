package com.test.silentcapture;

public interface IDeliver {
    /**
     * Deliver execution.
     *
     * @Params file: the full path of captured file, which will be as delivery attachment.
     *
     * Return value:
     * - 1: deliver successfully;
     * - 0: deliver failed, needs re-delivery;
     * -<0: deliver failed, no re-delivery;
     */
    public int deliver(String file);
}
