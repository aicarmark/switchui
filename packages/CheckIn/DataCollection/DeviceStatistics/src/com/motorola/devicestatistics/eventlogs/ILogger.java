/**
 ** Copyright (C) 2010, Motorola, Inc,
 ** All Rights Reserved
 ** Class name: ILogger.java
 ** Description: What the class does.
 **
 ** Modification History:
 ***********************************************************
 ** Date           Author       Comments
 ** Nov 2, 2010        bluremployee      Created file
 ***********************************************************
 **/
package com.motorola.devicestatistics.eventlogs;

/**
 *  * @author bluremployee
 *   *
 *    */
public interface ILogger {

    void log(int source, String type, String id, String log);
        
    void log(int source, int type, String id, String log);
                
    void checkin();

    void reset();
}

