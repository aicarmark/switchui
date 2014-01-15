package com.motorola.mmsp.taskmanager;

import java.text.Collator;
import java.util.Comparator;

class DisplayNameCompare implements Comparator<ProcessAndTaskInfo> {
        public DisplayNameCompare() {
        }

        public final int compare(ProcessAndTaskInfo TaskInfo1,ProcessAndTaskInfo TaskInfo2) {
            
            return sCollator.compare(TaskInfo1.appname, TaskInfo2.appname);
        }
        private final Collator   sCollator = Collator.getInstance();
}