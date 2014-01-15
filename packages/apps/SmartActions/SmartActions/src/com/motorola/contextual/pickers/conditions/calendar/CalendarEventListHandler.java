package com.motorola.contextual.pickers.conditions.calendar;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.Intent;
import android.util.Xml;

import com.motorola.contextual.smartprofile.CommandHandler;
import com.motorola.contextual.smartrules.R;

/**
 * This class extends CommandHandler for handling list command
 *
 * @author wkh346
 *
 */
public class CalendarEventListHandler extends CommandHandler implements
        CalendarEventSensorConstants {

    @Override
    protected String executeCommand(Context context, Intent intent) {
        context.sendBroadcast(getListResponse(context, new ArrayList<String>(),
                new ArrayList<String>(), intent.getAction(),
                context.getString(R.string.calendareventsensor)), PERM_CONDITION_PUBLISHER_ADMIN);
        return SUCCESS;
    }

    /**
     * This method constructs the list_response intent
     *
     * @param context
     *            - the application's context
     * @param configs
     *            - the list of configuration strings
     * @param descriptions
     *            - the list of description strings
     * @param publisherKey
     *            - the publisher key
     * @param newStateTitle
     *            - the name of condition publisher
     * @return - the list_response intent
     */
    public static Intent getListResponse(Context context, List<String> configs,
            List<String> descriptions, String publisherKey, String newStateTitle) {
        if (configs != null && descriptions != null) {
            int size = configs.size();
            XmlSerializer serializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            try {
                serializer.setOutput(writer);
                serializer.startDocument("utf-8", null);
                serializer.startTag(null, TAG_CONFIG_ITEMS);
                for (int index = 0; index < size; index++) {
                    serializer.startTag(null, TAG_ITEM);
                    serializer.startTag(null, TAG_CONFIG);
                    serializer.text(configs.get(index));
                    serializer.endTag(null, TAG_CONFIG);
                    serializer.startTag(null, TAG_DESCRIPTION);
                    serializer.text(descriptions.get(index));
                    serializer.endTag(null, TAG_DESCRIPTION);
                    serializer.endTag(null, TAG_ITEM);
                }
                serializer.endTag(null, TAG_CONFIG_ITEMS);
                serializer.endDocument();

                Intent listIntent = new Intent(ACTION_CONDITION_PUBLISHER_EVENT);
                listIntent.putExtra(EXTRA_EVENT_TYPE, LIST_RESPONSE);
                listIntent.putExtra(EXTRA_PUB_KEY, publisherKey);
                listIntent.putExtra(EXTRA_CONFIG_ITEMS, writer.toString());
                listIntent.putExtra(EXTRA_NEW_STATE_TITLE, newStateTitle);
                return listIntent;
            } catch (Exception exp) {
                exp.printStackTrace();
                return null;
            }
        }
        return null;
    }

}
