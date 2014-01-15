package com.motorola.mmsp.motohomex;

import android.content.Context;

/*changed by bvq783 from PagedTransitionFactory to TransitionFactory for entering transition */
public class WorkspacePagedTransitionFactory extends TransitionFactory {
    public WorkspacePagedTransitionFactory(Context context, String key) {
        super(context, key);
        // TODO Auto-generated constructor stub
    }

    public PagedTransition creatPagedTransition(int type) {
        switch (type) {
        /*2012-6-20, modify by bvq783 for switchui-1569*/
        case 0:
            //return PagedTransitionDefault.getInstance();
            return PagedTransitionBoxIn.getInstance();
        case 1:
            return PagedTransitionDoor.getInstance();
        /*case 2:
            return PagedTransitionBoxOut.getInstance();
        case 3:
            return PagedTransitionBoxIn.getInstance();*/
        case 2: //4:
            return PagedTransitionTwirling.getInstance();
        /*case 5:
            return PagedTransitionRotate.getInstance();*/
        case 3: //6:
            return PagedTransitionRotateSmooth.getInstance();
        /*2012-6-20, modify end*/
        /*case 7:
            return PagedTransitionJump.getInstance();*/
        default:
            throw new RuntimeException("Unknown type id " + type + "!");
        }
    }

    /*added by bvq783 for entering transition */
    public EntryTransition creatEntryTransition(int type) {
        return null;
    }
}
