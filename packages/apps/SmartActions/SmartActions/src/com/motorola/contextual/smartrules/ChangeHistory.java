package com.motorola.contextual.smartrules;

/**
 * v2.0.1 - Suren
 *   - Porting from Stable-6 into the one APK changes for Smart Profile complete.
 *
 * v2.0.2 - Suren
 *   - IKINTNETAPP-444: Add and update icons for Stable-7 from CxD.
 *
 * v2.0.3 - Rahul
 *   - Ported from Stable-6 into the one APK changes for Smart Rules. (Need to still
 *      port the changes made in rulesimporter.rules file).
 *
 * v2.0.4 - Boby Iyer
 *   - Merge from Stable6 rulesimporter XML
 *   - IKSTABLE6-13009 : Smart Action changes as per CxD reviews
 *   - IKSTABLE6-13460 : XML Changes to add Motion Sensor Trigger
 *   - IKSTABLE6-13606 :  Suggested sleep rule will not work if accepted from suggestion dialog
 *   - IKSTABLE6-13745 :  Low Battery Save Text
 *
 * v2.0.5 - Suren
 *   - IKSTABLE6-14388 - Changes for Sensor Rule State Listener
 *      - Upgraded the database from 26 to 27 to add a new view and index for Condition table
 *          publisher key (15th Sep 2011 and targeted for Stable-6).
 *      - Added a new view TriggerStateCountView for the count of enabled active triggers in the query set.
 *      - Send notifications to trigger publishers when to start and stop listening to trigger
 *          state changes based on the count of enabled active triggers in the DB.
 *   - IKSTABLE6-14879 - Changes for Sensor Rule State Listener to send notifications to
 *      trigger publishers when the VSM init complete intent is received.
 *
 * v2.0.6 - Suren
 *   - IKSTABLE6-15375: Location Inference fixes
 *      - Inference of Work and Home was broken (Insert to Location Sensor
 *          DB is broken as the accuracy was not being written).
 *      - Change the logic to infer home and work based on the number of inferred locations.
 *      - URI broadcasted to rules engine for suggestion of work or home rule corrected.
 *
 * v2.0.7 - Suren
 *   - IKSTABLE6-15634: Limit user to 30 enabled automatic rules.
 *      - Display dialog when the limit is reached and user tries to add a new rule.
 *      - Display dialog when the limit is reached and user tries to enable a disabled
 *          automatic rule from either Landing Page or Puzzle Builder.
 *      - Created a virtual view to return the count of visible enable automatic rules.
 *
 * v2.0.8 - Boby Iyer
 *   - IKSTABLE6-15888 : Layout and String fixes
 *
 * v2.0.9 - Boby Iyer
 *   - IKINTNETAPP-447 : Remove rules not required for Stable7
 *
 * v2.0.10 - Boby Iyer
 *   - IKINTNETAPP-447 : Enable Second Night Time Battery Saver Suggestion
 *
 * v2.0.11 - Suren
 *   - IKSTABLE6-16442: Sensor State Rule State Listener Changes for adding, deleting
 *      and editing rules.
 *   - IKSTABLE6-16449: Sort landing page rules list in a nice format.
 *   - IKSTABLE6-16704: Fix the rule key sent when a rule exits and is at the top of the
 *      conflicts stack.
 *      - Change logging to the debug viewer to remove unwanted intent logged.
 *
 * v2.0.12 - Suren
 *  - IKINTNETAPP-451: HSS7 changes for Sample Rules UI.
 *      - DB upgraded from 27 to 28 to add new filed SampleFkOrCount to rule table.
 *      - Added new virtual view RuleCloneView to clone a complete rule.
 *      - Show "Added" label for sample rules when user adopts a sample rule.
 *      - Do not delete sample rules from the sample rules list.
 *      - Moved business level classes into the business package.
 *      - Copy rule and adopt a sample rule to use the new rule clone view to make
 *          a copy of the rule.
 *      - Moved Conflicts into it's own class.
 *
 * v2.0.13 - Boby Iyer
 *  - IKINTNETAPP-447 : Meeting Rule sample/suggestion
 *
 * v2.0.14 - Suren
 *  - IKINTNETAPP-453: Correct the icon shown for the dock dialog.
 *  - IKINTNETAPP-454: Add the new icons to the icon selection grid.
 *  - IKINTNETAPP-455: Correct the VIP Caller Mode icon in the manifest and dialog.
 *  - Update icons for the rules builder blocks and states.
 *  - Update the text shown for about and the landing screen.
 *
 * v2.0.15 - Boby Iyer
 *  - IKSTABLE6-17893 : Low Battery Saver Suggestion Text
 *  - IKINTNETAPP-461 : Suggestion Card changes for Stable7
 *
 * v2.0.16 - Suren
 *  - IKSTABLE6-18035: Changes to icons and landing page layout as per CxD
 *
 * v2.0.17 - Suren
 *  - IKSTABLE6-18072: Changes to have the app run in com.motorola.system.process queue.
 *  - IKSTABLE6-16704: Change debugging direction to out.
 *
 * v2.0.19 - Suren
 *  - IKINTNETAPP-470: Sample Rules HSS7 changes.
 *      - Add logic to avoid multiple clicks on the rules list.
 *      - Add logic to handle stray entries in the rules list for home key press.
 *      - Modify the layout to show Added text in the first line of the row.
 *      - Change the background color of the added text shown in the list.
 *      - Same changes applicable for copy rule.
 *      - Set the Source to User and Flags to an empty string when rule is cloned.
 *      - Added a copy utility function to copy file from one location to the other.
 *  - IKINTNETAPP-471: Hide Add Location button if ILS is not installed (Phoenix Change)
 *
 * v2.0.20 - Boby Iyer
 *  - IKINTNETAPP-447 : Sample Rule changes to fix VSENSOR intent
 *
 * v2.0.21 - Suren
 *  - IKINTNETAPP-473: Sample Rules HSS7 changes.
 *      - Move the clone rule into a thread to avoid ANR's.
 *      - Fixes the issue of progress dialog not showing up in Landscape mode.
 *      - Remove logging the database version in the log of openHack to avoid
 *          a thread wait ANR.
 *      - Add the copyFiles code to the DB upgrade for Version 28.
 *  - IKINTNETAPP-471: Hide Change Location Button in Edit Location activity if
 *      ILS is not installed (Phoenix Change).
 *  - IKSTABLE6-18960: Landing Page to show View Samples always.
 *
 * v2.0.20 - Boby Iyer
 *  - IKINTNETAPP-447 : Sample Rule changes to fix VSENSOR intent
 *
 * v2.0.23 - Vignesh karthik-a21034
 *  - IKINTNETAPP-472 : Moving Smart Actions to new VSM via content providers
 *
 * v2.0.24 - Boby Iyer
 *  - IKSTABLE6-18676 : Move sendMessageToNotificationManager out of Receiver
 *
 * v2.0.25 - Boby Iyer
 *  - IKINTNETAPP-447 : Sleep Rule changes
 *
 * v2.0.26 - Visal Kith
 *  - IKSTABLE6-18450 : Changed to use PNG compression for wallpaper
 *
 * v2.0.27 - Boby Iyer
 *  - IKSTABLE6-19538 : Add Motorola Logo
 *
 * v2.0.28 - Visal Kith
 *  - IKMAIN-31135 : Remove private resources & rework on wallpaper chooser
 *
 * v2.0.29 - Rahul Pal
 *  - IKMAIN-29987: Fixed checking for non empty array before accessing
 *                         index in CheckStatusActivity
 *
 * v2.0.30 - Boby Iyer
 *  - IKINTNETAPP-447 : XML changes for Home, meeting and Low Battery rules
 *
 * v2.0.32 - Visal Kith
 *  - IKMAIN-31519 : Start voice announce when receving incoming call or text.
 *                   Also changed action SMS_RECEIVED_SOTRED to SMS_RECEIVED
 *
 * v2.0.33 - Rahul Pal
 *  - IKINTNETAPP-476 : Stable7 enhancements, new work
 *                      Enablers for audio, blocks anim interactions
 *                      Phoenix related check for matching pck mgr
 *                      for updating intent from tuple
 *
 * v2.0.34 - Rahul Pal
 *  - IKMAIN-31749 : Check for valid object before referencing, breakage on new feature dev fix
 *                   Also added missing check for pck mgr before updating actionFireUri
 *                   corresponding to the Phoenix compliance/OTA upgrade compatibility
 *
 * v2.0.36 - Visal Kith
 *  - IKMAIN-31823 : Added a null check to prevent null pointer exception when
 *                   querying active wallpaper list.
 *
 * v2.0.38 - Suren
 *  - IKSTABLE6-20437: Add "Last Active" status in title bar for Action Builder.
 *  - IKSTABLE6-21214: Append the region to the locale of the help URI.
 *  - IKSTABLE6-21237: Add new landing page and about graphics from CxD.
 *  - IKMAIN-32212: Home inference was not working due to error in Manifest file.
 *  - IKSTABLE6-21524: Block text changes as per CxD discussions.
 *  - General clean up of unused imports.
 *
 *
 * v2.0.39 - Rahul Pal
 *  - IKINTNETAPP-476 : Stable7 enhancements, new work, fix for issues reported
 *                      on earlier Stable 7 new work push
 *
 * v2.0.42 - Suren
 *  - IKMAIN-31675: Eliminate strict mode messages and move the query for active
 *                  settings controlled to the provider instead of raw query.
 *
 * v2.0.43 - Suren
 *  - IKMAIN-31675: Add code to create view in onCreate() of the DB helper if no
 *                  DB exists.
 *
 * v2.0.42 - Boby Iyer
 *  - IKINTNETAPP-480 : Fix the bug with encoded string
 *
 * v2.0.48 - Visal Kith
 *  - IKINTNETAPP-458 : changed to separate each item by 'and' instead of ','. And disable save
 *                     button if no item selected.
 *
 * v2.0.49 - Boby Iyer
 *  - IKMAIN-32734 : Auto-Text Reply should be stateless; fix in XML
 *
 * v2.0.50 - Suren
 *  - IKMAIN-33235: Update title string in delete rule dialog.
 *  - IKMAIN-33131: Update Smart Actions string in check status activity.
 *  - IKINTNETAPP-484: Set visibility to GONE instead of INVISIBLE in sample rules.
 *
 * v2.0.51 - Suren
 *  - IKMAIN-33481: Correct string value when 2 or more calls option is selected for missed calls.
 *  - IKMAIN-33461: Soft keypad to start with cap letters for timeframe name.
 *  - IKMAIN-33449: User feedback for disable all rules option in Landing Page.
 *  - IKMAIN-33193: Display Timeout options to match that of settings.
 *  - IKINTNETAPP-487: Copied rule with non-ASCII characters should work fine.
 *  - Sample rules copy should not have # 1 for the first time adoption.
 *
 * v2.0.53 - Suren
 *  - IKSTABLE6-21888: Synchronize processing of Rule State changes between MM and QA (Automatic Rules)
 *  - IKSTABLE6-21888: Synchronize processing of Rule State changes between MM and QA (Part 2)
 *      - Handle manually disabling and enabling of rules from Landing Page and Rules Builder.
 *      - Deletion of active rules from Landing Page and Rules Builder.
 *  - IKSTABLE6-23300: Replaced timer based interaction between ConditioBuilder / GraphicsActivity
 *                     with message passing/Intent based
 *
 * v2.0.54 - Suren
 *  - IKINTNETAPP-500: Mark accepted sample and suggested rules as enabled.
 *
 * v2.0.55 - Suren
 *  - IKMAIN-33936: Update the title displayed for add trigger/action list
 *  - IKMAIN-33928: Update text strings used in dialogs.
 *  - IKMAIN-33591: Restrict edit text box in website action activity to one line.
 *
 * v2.0.56 - Rahul Pal
 *  - IKMAIN-31430 IKMAIN-32977 IKSTABLE6-24115 IKMAIN-33721 IKMAIN-33018 IKINTNETAPP-476
 *      Bug fixes in GraphicsActivity on Audible feedback, Blocks slide gesture, interactions
 *
 * v2.0.57 - Suren
 *  - IKSTABLE6-24286: Store instance state to avoid force close when app is killed and
 *                      and onActivity result needs to be handled.
 *  - IKMAIN-34309: Sample/Suggested clone rules to be enabled only when user accepts them.
 *  - IKMAIN-30838: Handle screen rotation and avoid refetch of the help page.
 *  - IKMAIN-34444: Add app version to the debug data written to debug viewer.
 *
 * v2.0.58 - Rahul Pal
 *  - IKMAIN-33013 IKMAIN-33200 IKMAIN-34054 IKSTABLE6-24026 IKSTABLE6-24281:
 *      Rule interactions while saving and editing active rules
 *
 * v2.0.61 - Suren
 *  - IKSTABLE6-24776: Change logic to read action publisher meta data from the
 *      package manager and not from the DB.
 *
 * v2.0.62 - Suren
 *  - IKSTABLE6-24776: Fix crash seen due to v2.0.61 and also add settings intent
 *      meta data for Wall Paper and Voice Announce activities.
 *
 * v2.0.63 - Rahul Pal
 *  - IKSTABLE6-24820 Actions in error state cannot be reconfigured
 *
 * v2.0.64 - Rahul Vaish
 *  - IKINTNETAPP-449 ICS related changes in Calendar Events trigger
 *
 * v2.0.65 - Aman Bakshi
 *  - IKINTNETAPP-449 Force Close fix for Timeframe, BT, Wifi and Launch Website
 *
 * v2.0.66 - Suren
 *  - Fix the database issue seen when trying to clone a sample rule.
 *
 * v2.0.67 - Rahul Pal
 *  - IKSTABLE6-24633: Implementation of black list to remove any trigger/action
 *  - IKINTNETAPP-502: Media change for suggestion indicator in RulesBuilder blocks
 *
 * v2.0.68 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 1
 *      - Removed old Motorola framework ActionBar code and drawable resources.
 *      - Added the new ICS ActionBar to the UI.
 *      - Landing Page ActionBar Part 1 completed.
 *      - My Profile layout updated.
 *
 * v2.0.69 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 2
 *      - Added the ICS ActionBar for Edit use case (Save and Cancel).
 *      - Updated the Rules Builder code to show the ICS Action Bar.
 *      - Updated the Locations List Activity to show the ICS Action Bar.
 *      - Location Consent Dialog corrected to point to the right settings.
 *      - Notification for active rules updated to the latest spec.
 *      - Order of Landing Page menu items updated.
 *      - Settings activity title changed.
 *
 * v2.0.70 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 3
 *      - Database open/close to go through the openhack() and closeHack() calls
 *          as the DB is still being opened for write even when requested to open
 *          for read in ICS baseline.
 *      - Fixed orientation changes crash in Landing Page (remove references to old code)
 *          and in Locations List (add default constructor to EditFragment).
 *
 * v2.0.71 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 4
 *      - Removed the translucent theme and extra padding from Suggestion dialog.
 *      - Updated the error message displayed when no locations are present.
 *
 * v2.0.72 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 5
 *      - Updated the translucent themed dialogs with ICS theme Theme.Holo.Dialog.Alert.
 *
 * v2.0.73 - Bhavana
 *  - IKINTNETAPP-449: ICS Actionbar changes for Missed Call, TimeFrame, BT, WiFi, Send Message, Notifications
 *
 * v2.0.74 - Aman
 *  - IKINTNETAPP-449: ICS Changes for actions/preconditions
 *
 * v2.0.75 - Rahul Pal
 *  - IKMAIN-33453 - Window leak and Memory leak fix in RulesBuilder
 *
 * v2.0.76 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 6
 *      - Updated suggestion dialog with the backgrounds and color.
 *      - Fixed crash in Landing Page when selecting Samples.
 *      - Updated the Check Status Activity with the colors.
 *
 * v2.0.77 - Bhavana
 *  - IKINTNETAPP-449: ICS Actionbar changes for Calendar, UI changes for Auto Reply SMS, Send Message, TF
 *
 * v2.0.78 - Aman
 *  - IKINTNETAPP-449: ICS Changes for Playlist, SMS, Auto Reply, Timeframes and Addressing widget
 *
 * v2.0.79 - Rohit Malaviya
 *  - IKINTNETAPP-510 - Port MR2 changes to ICS for Rules Importer, Condition Builder, Suggestions and
 *                      Inference Manager
 * v2.0.80 - Bhavana
 *  - IKINTNETAPP-449: ICS UI Changes for most of the screens - Part2
 *
 * v2.0.81 - Rahul Pal
 * -  IKINTNETAPP-502 Avoid overwriting of Suggestion status
 *    IKMAIN-33965    Enable Manual confirm dialog in case of changing rule icon before saving
 *    IKINTNETAPP-511 Enabling utility for patching security hole, by limiting action and triggers
 *                    to a set of configurable packages
 *
 * v2.0.82 - Rahul Vaish
 *  - IKINTNETAPP-494 Missed Calls, Auto Reply Text, VIP Caller Mode changes in ICS
 *
 * v2.0.83 - Bhavana
 *  - IKINTNETAPP-449: ICS UI Changes for most of the screens - Part3
 *
 * v2.0.84 - Rahul Vaish
 *  - IKINTNETAPP-494 Missed Calls, Auto Reply Text, VIP Caller Mode changes in ICS
 *
 * v2.0.85 - Rohit Malaviya
 *  - IKINTNETAPP-510 - Suggestions UI changes for ICS
 *
 * v3.0.0.01 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 7
 *      - Edit Location activity designed as per ICS specs.
 *      - + sign to be shown for Timeframes activity.
 *      - Menu button to show up menu's for Landing Page and Rules Builder.
 *      - Design Locations List as per ICS specs.
 *
 * v3.0.0.02 - Rahul Vaish
 *  - IKINTNETAPP-449 Wi-Fi Connection changes - On ICS the shaded item doesn't look good
 *
 * v3.0.0.03 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 8
 *      - + Sign FC for Timeframes fixed.
 *      - Layouts updated with the blue color for ICS.
 *      - Added new hdpi drawables from CxD.
 *      - Removed the grey background from rules builder.
 *      - Save button disabled for an empty rule and rule with just triggers.
 *
 * v3.0.0.04 - Rahul Vaish
 *  - IKINTNETAPP-449 disable the save button in dialog when all calendars are unselected,
 *    formatted action bar changes, removed commented code
 *
 * v3.0.0.05 - Rahul Vaish
 *  - IKINTNETAPP-519 Timing string changes in Reminder, Send Text Message, and Launch Website actions
 *
 * v3.0.0.06 - Rahul Vaish
 *  - IKINTNETAPP-519 Set hint text for Missed Calls addressing widget
 *
 * v3.0.0.07 - Vignesh karthik
 *  - IKINTNETAPP-519 Timing preferences related changes on three actions - Send message/website/reminder
 *
 * v3.0.0.08 - Rahul Vaish
 *  - IKINTNETAPP-519 Bluetooth Device trigger crash on ICS resolved
 *
 * v3.0.0.09 - Rahul Vaish
 *  - IKINTNETAPP-519 Positive button text changes in preference dialogs in TimeFrames trigger
 *
 * v3.0.0.10 - Vignesh karthik
 *  - IKINTNETAPP-521 Update Car-Driving sample rule to be compliant with specs
 *
 * v3.0.0.12 - Rahul Vaish
 *  - IKINTNETAPP-519 'from' string changes in calendar events trigger
 *
 * v3.0.0.13 - Bhavana
 *  - IKINTNETAPP-519 - Code cleanup for Preconditions and Actions
 *
 * v3.0.0.14 - Rahul Vaish
 *  - IKINTNETAPP-519 error string changes for Launch Application and Play a playlist actions
 *
 * v3.0.0.15 - Suren
 *  - IKINTNETAPP-522: Handle the double click on icon in puzzle builder to edit an icon.
 *  - IKINTNETAPP-508: ICS Changes Part 9
 *      - Added new graphics from CxD.
 *      - Landing Page, Samples, Check Status, Location List layouts updated as per CxD spec.
 *      - Cleaned up unused imports.
 *      - Removed unused menu items from the fragment menus.
 *      - Show Cancel button when locations list is empty and launched from puzzle builder.
 *      - Add sync icon back.
 *      - Make changes for rules builder as per new CxD layout spec.
 *
 * v3.0.0.16 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 10
 *      - Fix force close in edit timeframe activity.
 *
 * v3.0.0.17 - Saral Kumar
 *  - IKINTNETAPP-523 Rules should not be restored if clear data is performed via application manager
 *
 * v3.0.0.18 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 11
 *      - Modify layouts as per CxD design spec.
 *      - Fix monkey test FC's in SmartRulesService, LandingPageActivity and GraphicsActivity.
 *      - Cleaned up dead code and unused variables.
 *
 * v3.0.0.19 - Bhavana
 *  - IKINTNETAPP-519 - Padding/marging changes for Preconditions and Actions
 *
 * v3.0.0.20 - Rahul Vaish
 *  - IKINTNETAPP-519 Triggers, Actions names changed along with internal strings
 *    according to ics lite document
 *
 * v3.0.0.21 - Saral Kumar
 *  - IKINTNETAPP-519 - Suggestion & Suggestion Inbox dialog changes
 *
 * v3.0.0.22 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 12
 *      - EditLocationLayout speced as per ICS design.
 *      - Fixed About and No rules landing page layout issues.
 *      - New drawables added.
 *      - Modified Landing Page, Check Status, Samples, Location List, Timeframes
 *          Rules Builder layouts as per CxD specs.
 *
 * v3.0.0.24 - Bhavana
 *  - IKINTNETAPP-519 - Minor UI fixes after CXD review
 *
 * v3.0.0.25 - Sreekanth
 * - IKCTXTAW-403 and IKCTXTAW-428 :
 *   - VSM is made non persistent. VSM is changed to IntentService from Service.
 *   - onUpgrade for upgrading the VirtualSensor database from Stable6-MR2 to ICS
 *
 * v3.0.0.26 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 13
 *      - Minor UI updates.
 *  - IKHSS7-3753: Fix strict mode violations in Smart Actions code.
 *
  * v3.0.0.27 - Suren
 *  - IKINTNETAPP-508: Fix the issue of not able to select the rule icon again to pick a new icon
 *      after selecting an icon in rules builder.
 *
 * v3.0.0.28 - Rahul Vaish
 *  - IKINTNETAPP-533 issues in Mobile dock
 *
 * v3.0.0.29 - Aman
 *  - IKINTNETAPP-519: Propagating Voice Announce changes to ICS for ringer revert.
 *
 * v3.0.0.30 - Bhavana
 *  - IKINTNETAPP-519: Title changes according to ics lite doc for FW actions.
 *
 * v3.0.0.31 - Saral Kumar
 *  - IKINTNETAPP-510 - Fix for MissedCall trigger not working post upgrade from GB
 *
 * v3.0.0.32 - Suren
 *  - IKINTNETAPP-508: ICS Changes Part 14
 *      - Updated drawables from CxD.
 *      - Cleaned up unused drawables, layouts, colors and styles.
 *      - Fixed issue of a manual sample rule showing in Ready state.
 *      - Cleaned up unused imports and dead code.
 *      - Fixed Disabled Actions/Triggers issue seen.
 *      - List gradient issue fixed.
 *  - IKHSS7-3863: Fix the touch issue on Landing Page action bar app icon.
 *
 * v3.0.0.33 - Rohit Malaviya
 *  - IKINTNETAPP-510 - Suggestions UI fine tuning
 *
 * v3.0.0.34 - Saral Kumar
 *  - IKINTNETAPP-510 - Minor meeting inference updates to work in ICS
 *
 * v3.0.0.36 - Boby Iyer
 *  - IKHSS6-874 : Localization changes for Target state
 *
 * v3.0.0.37 - Bhavana
 *  - IKINTNETAPP-536: Propagation of timeframe issue
 *
 * v3.0.0.39 - Rahul Pal
 *  - IKHSS6-1544 : Changes to avoid blank rule can be saved
 *  - IKHSS6-1225 : MissedCall and AutoSend sms filter lists
 *  - IKHSS6-1627 : Locale issue for Trigger name being changed for added rule
 *
 * v3.0.0.40 - Suren
 *  - IKHSS6-1343: Fix rule icon selection issue in rules builder.
 *
 * v3.0.0.41 - Aman
 *  - IKINTNETAPP-519: Website launch action UI change
 *
 * v3.0.0.42 - Rohit Malaviya
 *  - IKINTNETAPP-510 - Update Suggestions Verbiage
 *
 * v3.0.0.43 - Suren
 *  - IKHSS6-1926: Fix capitalization of strings.
 *
 * v3.0.0.44 - Aman
 *  - IKINTNETAPP-519: Wallpaper transparency issue fix and AW edit dialog UI updated
 *
 * v3.0.0.45 - Rahul Vaish
 *  - IKINTNETAPP-545 Rule created with All Calendars as trigger doesn't become active
 *    after rebooting the phone during the meeting event
 *
 * v3.0.0.46 - Rahul Vaish
 *  - IKINTNETAPP-546 rule with Any Dock as trigger doesn't become active in some cases
 *
 * v3.0.0.47 - Rohit Malaviya
 *  - IKINTNETAPP-510 - Fine tuning sample/suggestion for ICS-Lite
 *
 * v3.0.0.48 - Aman
 *  - IKINTNETAPP-519: Addressing widget button width fix
 *
 * v3.0.0.49 - Rohit Malaviya
 *  - IKHSS6-1269 - Change in onPause logic for suggestion card
 *
 * v3.0.0.50 - Suren
 *  - IKINTNETAPP-508: Centralize the DebugViewer calls and log internal messages to the
 *      provider only for non-production builds.
 *
 * v3.0.0.51 - Suren
 *  - IKHSS6-1265: Make list elements selectable via external keypads.
 *  - IKHSS6-2322: Dynamically toggle landing page menu based on user visible rules.
 *  - IKHSS6-3043: Live wallpaper warning dialog updated as per CxD discussion.
 *  - IKHSS6-3101: Up navigation in rules builder has to behave like Cancel button.
 *  - IKSTABLE6-26362 Fix debug viewer logs for multiple use cases and do not send
 *      stop to Motion Sensor trigger if there is a location trigger being used in a
 *      user accepted rule.
 *
 * v3.0.0.53 - Rohit Malaviya
 *  - IKINTNETAPP-543 - Layout for the "Get Started" dialog revised per CxD request
 *
 * v3.0.0.54 - Aman
 *  - IKHSS6-3132: Change IME options and soft keyboard visibility similar to other apps in ICS
 *
 * v3.0.0.55 - Suren
 *  - IKINTNETAPP-551: Changes to show Done button in action bar.
 *      - Make changes for the Fragment layer to replace Save with Done
 *          and set visibility of Cancel to false.
 *
 * v3.0.1.01 - Suren
 *  - IKINTNETAPP-552: Implement single list Add new rule HSS7 spec.
 *  - IKHSS6-4350: Rules builder should show save and not done (changes made in 3.0.0.55).
 *
 * v3.0.1.02 - Suren
 *  - IKHSS6-4603: Update with new media from CxD.
 *      - Fix text alignment issue in rules builder rule status line.
 *  - IKINTNETAPP-552: Close cursor in the Add rule fetch data.
 *      - Changes to show the icon for the blank rule.
 *      - Changes to launch this new AddRuleActivity from notification if there are
 *          multiple new suggestions notification.
 *
 * v3.0.1.03 - Suren
 *  - IKHSS6-4603: Update with new media from CxD.
 *      - Fix alignment issue in Check Status screen.
 *  - IKINTNETAPP-552: Fix issue in KW defect when closing cursor for add rule fetch data.
 *  - IKINTNETAPP-553: Fix errors and warnings in the Smart Actions App
 *      - Fix KW errors in other modules
 *      - Make sure all Log.d are preceeded by a if(LOG_DEBUG) statement.
 *      - First round of cleaning up Log.i logs to Log.d logs.
 *      - First round of cleaning of lint errors/warnings.
 *
 * v3.0.1.04 - Aman
 *  - IKINTNETAPP-519: Turn off debug logging in Actions module
 *
 * v3.0.1.05 - Rahul Vaish
 *  - IKINTNETAPP-550 ui, functionality, resource related changes in ics full
 *
 * v3.0.1.06 - Boby Iyer
 *  - IKMAIN-35110 : Changes to SmartRuleService Synchronization.
 *
 * v3.0.1.07 - Aman
 *  - IKINTNETAPP-519: Adding contact photo to addressing widget
 *
 * v3.0.1.08 - Aman
 *  - IKINTNETAPP-519: Add signature to messages sent by Smart Actions
 *
 * v3.0.1.09 - Aman
 *  - IKINTNETAPP-519: Change Webtop travel adapter to Lapdock.
 *      - Auto reply text signature to be added only after user touches the EditText
 *      - Remove text changed listener in onDestroy for all applicable actions
 *
 * v3.0.1.10 - Aman
 *  - IKCTXTAW-436: String translation for preloaded timeframes
 *
 * v3.0.1.11 - Rahul Vaish
 *  - IKHSS7-6654 changed play notification to play a sound in Reminder action
 *
 * v3.0.1.12 - Aman
 *  - IKINTNETAPP-519: Addressing widget contact photo UI changes
 *
 * v3.0.1.13 - Suren
 *  - IKHSS7-6152: Hide 'Copy an existing rule' option when no rules in Landing Page.
 *  - IKHSS6-6212: Algin the rule icon properly in Check Status Activity.
 *  - IKHSS6-6214: Modify the logic for disabling all progress spinner to be dismissed.
 *  - IKINTNETAPP-552: Clear the suggestion notification and also set the first time dialog
 *      shared preference for Suggestions when user directly views the Add rule list.
 *  - IKINTNETAPP-553: Second round of Lint cleanup.
 *
 * v3.0.1.14 - Aman
 *  - IKHSS7-7354: Allow clicking done button only once in Missed Call trigger to avoid force close
 *
 * v3.0.1.15 - Rahul Vaish
 *  - IKINTNETAPP-558  description text appears incorrectly in puzzle builder if two or more contacts
 *    with same names are added in missed calls trigger
 *
 * v3.0.1.17 - Rahul Pal
 *  - IKHSS6-2281  Manual dialog confirmation issue fix,
 *    IKHSS7-6166  Fix for showing previous already configured action/condition
 *
 * v3.0.1.18 - Suren
 *  - IKHSS6-7654: Fix StrictMode violation for cursor close.
 *  - IKHSS6-7655: Fix Landing Page layout for when font size is increased to huge.
 *  - IKHSS6-7659: Fix padding between the status icon and status text on Landing Page.
 *  - IKHSS7-7133: Fix string for Add rule dialog - new rule subtext.
 *
 * v3.0.1.19 - Bhavana
 *  - IKHSS7-7811 : BR:ET:HSS6:Spyder: Virtual keypad doesn't disappear when Home key is pressed from Smart Actions
 *  - IKHSS7-7827 : BR:ET:HSS6:Spyder:All the entered numbers in send text message gets disappeared when following the interaction
 *
 * v3.0.1.20 - Aman
 *  - IKINTNETAPP-519: Adding Chips UI code
 *
 * v3.0.1.21 - Saral Kumar
 *  - IKHSS7-7882 Fix StrictMode violation for cursor close
 *
 * v3.0.1.22 - Suren
 *  - IKHSS6-7913: Show dialog when number of visible enabled automatic rules hits 30.
 *  - IKHSS6-7899: Remove negative padding to avoid the text being cut off.
 *
 * v3.0.1.23- Bhavana
 *  - IKHSS7-8637: Voice Announce doesn't work after market upgrade
 *
 * v3.0.1.24 - Saral Kumar
 *  - IKHSS7-8906:  Remove unused permission
 *
 * v3.0.1.25 - Suren
 *  - IKHSS6-8652: Fix force close issue when deleting a rule.
 *  - IKHSS7-8939: Make about screen a scroll view layout to fit all screen sizes.
 *  - IKHSS7-8562: Changes to make sure the rule is deleted when user selects home
 *                  key after deleting the rule.
 *  - IKHSS7-7688: Rule icon should be updated properly when changed in rules builder.
 *
 *  v3.0.1.26 - Rahul Pal
 *  - IKHSS6-5712: Check status made to show same text as Descritpion, point targetState to description
 *  - IKHSS6-8755: Saving progress dialog shows forever, add missing callback for Manual rule
 *  - IKHSS6-9300: Do not allow to save an empty rule on editing rule name
 *
 * v3.0.1.29 - Rahul Vaish
 *  - IKHSS6-10453: Race condition in voice announce action causes problem
 *
 * v3.0.0.30 - Rahul Pal
 *  - IKHSS6-1939: Reset/release MediaPlayer resources
 *  - IKHSS6-7351: ANR fix, move init/playback from main thread into worker thread
 *
 * v3.0.1.31 - Bhavana
 *  - IKHSS6-9897: BR:ET:HSS6:Spyder:Process com.motorola.contextual.smartrules Force close seen during the interaction
 *
 * v3.0.1.32 - Rahul Vaish
 *  - IKHSS7-8336: Strict mode disk write violation in callsmsreceiver
 *
 * v3.0.1.33 - Aman
 *  - IKHSS7-8028 Integrating chips UI with Smart Actions
 *
 * v3.0.1.34 - Suren
 *  - IKHSS6-7672: Change the list selector background to Halo ICS theme background.
 *  - IKHSS7-9598: Rule Icon changed should be updated accordingly in the Landing Page.
 *  - IKHSS7-9076: Mark rule as manual or automatic when clone is called before writing to DB.
 *
 * v3.0.1.35 - Aman
 *  - IKHSS6-10993: Release Media Player instance in Ringtone action
 *
 * v3.0.1.36 - Aman
 *  - IKHSS7-7524: Disable save option in timeframe name dialog if edit text is empty
 *
 * v3.0.1.37 - Aman
 *  - IKHSS6-10892: Lapdock intent extra corrected
 *
 * v3.0.1.38 - Suren
 *  - IKHSS6-11399: Change logic to check if poi name and poi address are the same.
 *  - IKHSS6-11398: Remove Fragment construcor override and replace with newInstance().
 *
 * v3.0.1.39 - Rahul Vaish
 *  - IKHSS7-7435: resolution for time consuming file operation in main thread in set wallpaper
 *
 * v3.0.1.40 - Aman
 *  - IKHSS6-12094: Auto reply text not to be displayed in Check status screen for Driving rule
 *
 * v3.0.1.41 - Rahul Vaish
 *  - IKHSS7-10156: resolution for strict mode read violation in calendarevents trigger
 *
 * v3.0.1.42 - Saral Kumar
 * - IKHSS7-11785: Minimize logging in InferenceManager, Rules Importer etc
 *
 * v3.0.1.43 - Rahul Vaish
 *  - IKHSS7-11453: resolution for race condition in wallpaper activity
 *
 * v3.0.1.44 - Aman
 *  - IKINTNETAPP-492: Integrate linking with contacts feature for Send SMS action
 *
 * v3.0.1.45 - Suren
 *  - IKHSS6-12369: Check status layout to be modifed to not overlap text.
 *
 * v3.0.1.46 - Rahul Vaish
 *  - IKINTNETAPP-578: Wallpaper doesn't get restored when progress dialog is canceled in
 *    Wallpaper action
 *
 * v3.0.1.47 - Aman
 *  - IKINTNETAPP-577: Fix crash in DatabaseUtilityService.
 *
 * v3.0.1.48 - Rahul Pal
 *  - IKHSS6-10624: Refresh Layout to re-measure and redraw description
 *  - IKHSS7-10115: Cant believe am fixing this, change 'Airplane Mode' to 'Airplane mode'
 *
 * v3.0.1.49 - Aman
 *  - IKHSS6-12744 Fix for BT trigger in case multiple devices are configured
 *
 * v3.0.1.50 - Aman
 *  - IKHSS6-12915 Fix for Auto reply text not getting disabled even after rule exits
 *
 * v3.0.1.51 - Rohit Malaviya
 *  - IKHSS6-10354 Update LBS suggestion text prologue.
 *
 * v3.0.1.52 - Aman
 *  - IKHSS6-12724 Fix for timeframe days of the week UI bug
 *
 * v3.0.1.53 - Rahul Vaish
 *  - IKHSS6-5963 resolution for issues observed during interaction of calendar events
 *    dialog with physical keyboard
 *
 * v3.0.1.54 - Bhavana
 *  - IKHSS6-13347 com.motorola.contextual.smartrules : java.lang.RuntimeException:
 *      An error occured while executing doInBackground()
 *
 * v3.0.1.55 - Rahul Vaish
 *  - IKHSS7-11448 changes in wallpaper action design
 *
 * v3.0.1.56 - Rahul Vaish
 *  - IKHSS6-13965 removal of few possible strict mode violations
 *
 * v3.0.1.57 - Suren
 *  - IKHSS7-14863 Widget related framework changes to support
 *      - Deleting a rule
 *      - Modifying a rule
 *      - Rule going on/off
 *      - Send init complete message out to widget
 *      - Process request from Widget during init complete
 *
 * v3.0.1.58 - Suren
 *  - IKHSS7-14863 Fix the FC seen due to class renamed and not properly corrected in
 *      Android Manifest
 *
 * v3.0.1.59 - Rahul Pal
 *  - IKINTNETAPP-586 Saving rule progress dialog doesnt go, use case was missing the callback intent from CB
 *  - IKHSS7-14437 Timeframe, multiple OR condition sensors were not getting re-created on disabling re-enabling
 *                 condition.
 *  - IKHSS7-11088 Options on virtual keyboard should not appear when not needed
 *
 * v3.0.1.60 - Aman
 *  - IKHSS7-14881 Fix for ANR in Chips UI
 *       - Remove old Addressing Widget code
 *
 * v3.0.1.61 - Saral Kumar
 *  - IKHSS7-12449 Data Backup Restore
 *
 * v3.0.1.62 - Aman
 *  - IKINTNETAPP-519 Move VIP Ringer specific code from StatefulActionReceiver
 *
 * v3.0.1.63 - Suren
 *  - IKHSS7-14863 Widget related framework changes to support
 *      - Enable/Disable an automatic rule.
 *      - Do not send the rule processed response if the rule was also marked for deletion.
 *
 * v3.0.1.64 - Suren
 *  - IKHSS7-11971: Moto care requirements for metrics
 *      - Source field to reflect the proper rule type
 *      - Dump the source field along with rule edited or not
 *      - DB upgraded to version 30 on 15th Mar 2012. Added the LAST_EDITED_DATE_TIME field
 *        to the rule table to keep track of when this rule was edited last.
 *  - Moved the old cursor release to after the new cursor is fetched to avoid a blank
 *    screen for a brief moment in Landing Page.
 *
 * v3.0.1.65 - Suren
 *  - IKHSS7-14863 Widget related framework changes to support
 *      - Remove the parcelable array list and send individual intents for widget init request/response.
 *      - Changes to send response to widget when an auto rule is clicked and the state
 *          changes from Ready to Disabled or Disabled to Ready.
 *      - Send the rule processed response to the widget only if the rule is visible to the user.
 *
 * v3.0.1.67 - Suren
 *  - IKHSS7-16007: Force close fixed due to orientation change in Add rule and copy rule activities.
 *  - IKHSS7-15475: Change the settings meta-data for GPS action.
 *  - IKHSS7-14845: Update icons for mdpi and xhdpi resolutions.
 *
 * v3.1.0.68 - Suren
 *  - IKHSS7-11971: Added tag should be shown for adopted samples.
 *      - Changed the version code and version name in Android Manifest.
 *
 *  * v3.1.0.69 - Rahul Pal
 *  - IKHSS6-14397 Change to decouple WiFi dependency from Location based rules  where WiFi Autoscan is supported
 *
 * v3.1.0.70 - Rahul Vaish
 *  - IKHSS7-17035 resolution for Activity getting destroyed on screen size change
 *
 * v3.1.0.71 - Suren
 *  - IKINTNETAPP-592: UI Abstraction Layer Changes Part 1
 *
 * v3.1.0.72 - Bhavana
 *  - IKHSS7-17063 PR: ET: Vanquish: UI mistake in Smart Actions Missed calls
 *
 * v3.0.1.73 - Aman
 *  - IKINTNETAPP-519 Remove old addressing widget resources
 *
 * v3.0.1.74 - Saral Kumar
 *  -  IKHSS7-17634 - Recover Backup Restore feature due to MotoCare Changes
 *
 * v3.0.1.75 - Suren
 *  - IKHSS7-17649: Accepted suggestions source should be set to SUGGESTED.
 *      - On Boot up and new rule creation notify condition publishers has a latent bug.
 *
 * v3.0.1.76 - Rahul Vaish
 *  - IKHSS7-17541 Silent checkbox was not getting checked on selecting volume as zero
 *
 *  v3.1.0.77 - Rahul Vaish
 *  - IKHSS7-16052 resolution for alignment issues in missed calls for arabic and hebrew.
 *    Pulling in the fix for IKHSS7-15401 for resolving the alignment issue in timeframes
 *
 * v3.1.0.78 - Suren
 *  - IKINTNETAPP-592: UI Abstraction Layer Changes Part 2
 *      - Implemented changes to Check Status and Landing Page activities to use the
 *          UI Abstraction layer calls.
 *      - Implemented disable all, insert, clone and delete of a rules.
 *
 * v3.1.0.79 - Vignesh karthik-a21034
 *  - IKHSS7-16839 PSF Introduction
 *
 * v3.1.0.80 - Suren
 *  - IKINTNETAPP-592: UI Abstraction Layer Changes Part 3
 *     - Clean up of comments from Part 2 changes.
 *     - Samples, Suggestions rules list fetch implemented.
 *     - Update of a rule implemented.
 *     - Redesigned delete of a rule.
 *     - Change rule state via icon press in the landing page.
 *
 * v3.1.0.81 - Qinghu Li
 *  - IKHSS7-19103 'Done' option is disabled after doing display off/on for "Auto reply text" action.
 * v3.1.0.82 - Qinghu Li
 *  - IKHSS7-15504:My current location name can not be changed in the smart profiles
 *      - Do not need update location name when only want to update PoiCellJsons.
 *
 * v3.1.0.83 - Bhavana
 *  - IKHSS7-19939 Timeframe rule is not becoming active state after it moves to ready state
 *
 * v3.1.0.84 - Rahul Vaish
 *  - IKHSS7-20014: Voice announce shall not happen once the call is picked up
 *
 * v3.1.0.85 - Suren
 *  - IKINTNETAPP-592: UI Abstraction Layer Changes Part 4
 *      - Refactor the design for abstarction.
 *      - Incorporate the comments from earlier reviews.
 *      - Added comments for documentation purposes.
 *
 * v3.1.0.86 - Rahul Vaish
 *  - IKHSS7-19566 Silent mode gets set to Mute instead of Vibrate for Work sample rule
 *
 * v3.1.0.87 - Aman
 *  - IKHSS7-21458 Ringer and VIP Caller actions updated according to ICS sound settings
 *
 * v3.1.0.88 - Suren
 *  - IKINTNETAPP-592: UI Abstraction Layer Changes Part 5
 *      - Implement the constructors for Action and Condition.
 *  - IKMAIN-38333: Fix force close due to class cast exception, make Rule class implement
 *      Parcelable to fix this.
 *
 * v3.1.0.89 - Rahul Pal
 *  - IKMAIN-38341 Fix for force close in UIAbstraction layer changes
 *  - IKINTNETAPP-592: UI Abstraction Layer Changes Part 5 - fixes
 *    Porting of following changes from HSS7
 *  - IKHSS6UPGR-3950 More Wifi loc Autoscan related changes, mostly bug-fixes
 *  - IKHSS7-15539 Rule icon change, to avoid rule state change in edit mode
 *  - IKHSS7-17608 Fix for breakage as result of IKHSS6-14397
 *  - IKHSS7-17559 Latent bug for Fragment commit, use commitAllowingStateLoss
 *
 * v3.1.0.90 - Rahul Vaish
 *  - IKHSS7-21996 Calendar events trigger shows 0 in edit mode if only one calendar was selected
 *
 * v3.1.0.91 - Suren
 *  - IKHSS7-22796: View created for adopted sample rules list based on rule key.
 *
 * v3.1.0.92 - Aman
 *  - IKHSS7-23569 Handle conflicts between two or more rules with VIP Caller Mode action
 *
 * v3.1.0.93 - Aman
 *  - IKHSS7-23964 BOTA upgrade fix for send text message action
 *
 * v3.1.0.94 - Rahul Vaish
 *  - IKHSS7-23961 Voice announce doesn't happen when head set is inserted
 *
 * v3.1.0.95 - Qinghu Li
 *  - IKHSS7-15694:Vanquish:SQL lite errors when a smart rule is triggered
 *
 * v3.1.0.96 - Qinghu Li
 *  - IKMAIN-38682:Qinara: Repeated strict mode disk write violation on smart rules when resizing drive mode widget
 *  - IKMAIN-38683:Qinara:Cursor leaks + repeated strict mode disk write violations are seen on smart actions process
 *
 * v3.1.0.97 - Qinghu Li
 *  - IKMAIN-38859:Visual keypad need pop up when choosing 'Selected contacts' in Auto reply text
 *
 * v3.1.0.98 - Rahul Vaish
 *  - IKHSS7-24609 calendar dialog was not getting reinitalized when cancelled on tapping outside
 *
 * v3.1.0.100 - Bhavana
 *  -   IKHSS7-25495 Force Close in Missed Call
 *
 * v3.1.0.101 - Aman
 *  - IKHSS7-25722 Ringer activity should not change the ringer volume
 *                 Backward compatibility for old Meeting rule
 *
 * v3.1.0.102 : Boby Iyer
 *  - IKINTNETAPP-598 : Bring the following changes to maindev-ics:
 *    Drive Mode Widget changes : IKHSS7-18474, IKHSS7-19234, IKHSS7-20046, IKHSS7-23229, IKHSS7-23529
 *    Suggestion Layout Changes : IKHSS7-17257,
 *    Defects : IKHSS7-24003, IKHSS7-26291
 *
 * v3.1.0.103 - Suren
 *  - IKINTNETAPP-600: Port UI Enhancement related changes and bugs from hss7
  *     - IKHSS7-23215: UI Enhancements Part 1
 *          - Landing Page updated to show separated lists, removed animation and individual
 *              click areas.
 *          - About, Starting Welcome Screens landscape and portrait implemented.
 *          - Color iconography media updated.
 *          - Rule icon list trimmed to only use color icons.
 *          - Add Rule, Copy Rule and Check Status layouts fixed as per new spec.
 *          - Landing Page welcome screen and About screen layouts updated.
 *          - Copy rule and Check status screens should show only new rule icons and not
 *              legacy icons.
 *          - Added default rule icon to the list of rule icons.
 *          - Rules builder updated as per UI Enhancements Spec.
 *          - Add Rule List to show copy option when Get Started is selected from About and
 *              rules are there on the landing page.
 *      - IKHSS7-23699: Get Started button click should be handled in About screen.
 *      - IKHSS7-23714: Force close when context menu is to be displayed for a rule.
 *      - IKHSS7-23631: Force close when back key is pressed and action bar commit is
 *          executed with a delay after onDestory() is called.
 *      - IKHSS7-23640: Strings corrected for adding actions and triggers in rules builder.
 *      - IKHSS7-24160: Catch exception due to Package Manager kill.
 *      - IKHSS7-24537: Set translatable=false for strings that do not need translation.
 *      - IKHSS7-25067: List headers in Landing Page and Add Rule List should not be
 *          selectable.
 *      - IKHSS7-25388: Fix force close due when trying to set the on off switch state.
 *      - IKHSS7-24305: Accepted Low Battery Suggestion should be in enabled state.
 *      - IKHSS7-24832: Protect the user from clicking on the On/Off switch in rules builder
 *
 * v3.1.0.104 - Saral Kumar
 *  - IKHSS7-25288 : After restoring the data, Unreadable characters is shown in the "My Rules".
 *
 * v3.1.0.106 - Rohit Malaviya
 *  - IKMAIN-39548: Publisher blacklisting support in RulesImporter
 *
 * v3.1.0.107 - Rohit Malaviya
 *  - IKHSS7-19909 New Architecture - Rule Publisher
 *
 * v3.1.0.108 - Saral Kumar
 *  -  IKHSS7-26045 After Restore Copied Sample rule is not getting listed in landing page
 *
 * v3.1.0.110 - Rahul Pal
 *    IKMAIN-39495
 *  - Porting of IKHSS7-22753 Avoid saving rules without any action
 *  - Porting of IKHSS7-23431 Widget interaction changes, creating new task for widget related usecase
 *
 * v3.1.0.111 - Bhavana
 *  - IKHSS7-16839 New Architecture - Sample rule issues
 *
 * v3.1.0.112 - Rahul Vaish
 *  - IKHSS7-16839 Meeting rule shall leave and start again at end of back to back meetings
 *
 * v3.1.0.114 - Suren
 *  - IKINTNETAPP-605 - Porting below changes from HSS7 to main-dev-ics)
 *      - IKHSS7-26743: Rule block status should update when in edit mode in rules builder.
 *      - IKHSS7-26307: Various layout fixes as requested by CxD. Add the blue line to the
 *          separated list headers.
 *      - IKHSS7-26209: Save button should be enabled in rules builder when a unconfigured
 *          block is connected.
 *      - IKHSS7-26056: Suggested rule with source set to Suggested should not be cloned.
 *      - IKHSS7-26041: Invoke TriggerStateService to notify publishers after rules are
 *          restored from backup.
 *      - IKHSS7-26742: Notify trigger publishers when an active rule is disabled manually
 *          by the user.
 *      - IKHSS7-27976: Active rule should be deleted and not just disabled. Icomplete fix
 *          made via IKHSS7-26742.
 *      - IKHSS7-28716: Make sure to enable an accepted suggestion.
 *      - IKHSS7-26633: Update dialog icons from CxD.
 *      - IKHSS7-28881: Update media in welcome screen.
 *
 * v3.1.0.115 - Rahul Vaish
 *  - IKHSS7-29231 Resolution for force close observed in play a playlist
 *
 * v3.1.0.116 - Suren
 *  - IKHSS7-28353: Icon should be updated when changed by the user.
 *  - IKHSS7-27059: On Off Switch should not be continuously changing states.
 *
 * v3.1.0.117 - Bhavana
 *  - IKHSS7-16839 New Architecture - WHEN_RULE_ENDS fix
 *
 * v3.1.0.118 - Rohit Malaviya
 *  - IKHSS6UPGR-9653 - Make Maps dependency optional &
 *                      Hide Location from "My Profile" if location is blacklisted
 *
 * v3.1.0.119 - Rahul Vaish
 *  - IKHSS7-30581 resolution for null pointer exception in play a playlist
 *
 * v3.2.1.0.121 - Suren
 *  - IKHSS7-27199: App name should not be translated to other languages and some minor UI
 *      fixes.
 *  - IKHSS6UPGR-9191: Logic change to notify the trigger publishers to start or stop
 *      listening to state changes (DB upgraded from version 31 to 32).
 *  - IKHSS7-26041: After rule restore from backup notify trigger publishers.
 *  - IKHSS7-29881: Handle orientation changes in Location List screen.
 *
 * v3.2.1.0.122 - Aman
 *  - IKHSS7-31338 Wallpaper action to show an error if wallpaper file is not found
 *
 * v3.2.1.0.123 - Sreelakshmi
 *  - IKMAIN-42170 Listen to SA_CORE_INIT_COMPLETE in InitCompleteBroadcastReceiver
 *
 * v3.2.1.0.124 - Aman
 *  - IKHSS7-31478 Phone number type added to chips widget dropdown
 *
 * v3.2.1.0.125 - Rohit Malaviya
 *  - IKHSS6UPGR-10144 Hide location-wifi correlation dialog when location trigger is blacklisted
 *
 * v3.2.1.0.126 - Rahul Vaish
 *  - IKHSS7-29834 Hint text in missed calls was not consistent with auto text reply and vip caller mode
 *
 * v3.2.1.0.127 - Aman
 *  - IKHSS6UPGR-10188 Conflict resolution between Ringer-Voice announce and Ringer-VIP Ringer
 *
 * v3.2.1.0.128 - Yanyang Xie
 *  - IKHSS7-30660 PR:ET:HSS7:QNR&VNQ: 'g' got cut off in action 'Launch application' when disable app and set to large text in Smart Actions
 *
 * v3.2.1.0.129 - Jun Li
 *  - IKMAIN-41902 PR: ET: SPD: Menu can be called out after deleting a rule in Smart Action
 *
 * v3.2.1.0.130 - Yanyang Xie
 *  - IKMAIN-42547 Fix main-dev-ics pheonix build issue for smartactions
 *
 * v3.2.1.0.131 - Aman
 *  - IKHSS6UPGR-10307 Ringer action in sample rules should not use hard coded max volume
 *
 * v3.2.1.0.132 - Rahul Vaish
 *  - IKHSS7-31688 backup restore was not working for Meeting rule with modified configuration
 *
 * v3.2.1.0.133 - Rahul Vaish
 *  - IKHSS7-32885 ellipsized the text in calendars dialog
 *
 * v3.2.1.0.134 - Rahul Vaish
 *  - IKHSS7-27098 Handling the use case of Calendar deletion in Calendar events trigger
 *
 * v3.2.1.0.135 - Saral Kumar
 *  - IKINTNETAPP-614 UI Controllder, XML New tag support
 *
 * v3.2.1.0.136 - Suren
 *  - IKHSS7-33535: Drive Rule Widget changes as per CxD Discussion and copy Drive rule
 *      changes from HSS7 to main-dev.
 *
 * v3.2.1.0.137 - Suren
 *  - IKHSS6UPGR-10578: Handle FC due to DB downgrade by overriding onDowngrade handler.
 *      Removed support for upgrade of DB versions before v26 (on May 21 2012).
 *
 * v3.2.1.0.138 - Aman
 *  - IKMAIN-43296 Error should be displayed only once on action blocks
 *
 * v3.2.1.0.140
 *  - IKHSS7-34363: Change the drive rule widget preview image.
 *  - IKMAIN-42155: Correct the icon used for Timeframes.
 *
 * v3.2.1.0.141 - Aman
 *  - IKMAIN-43971 Check for framework apk to be done at PSF_INIT_COMPLETE
 *
 * v3.2.1.0.143 - Aman
 *  - IKHSS7-35034 Support only single segment messages
 *
 * v3.2.1.0.144 - Aman
 *  - IKMAIN-44343 VIP Ringer activity force close fix
 *
 * v3.2.1.0.145 - Rahul Pal
 *  - IKMAIN-44356 Breakage fix for IKMAIN-43796
 *
 * v3.2.1.0.146 - Suren
 *  - IKHSS7-35112: Implement the 2x1 drive widget and change name to Drive Smart.
 *
 * v3.2.1.0.147 - Suren
 *  - IKMAIN-44635: Fix force close seen when 30 enabled automatic rules and add button is
 *      selected.
 *
 * v3.2.1.0.148 - Yanyang Xie
 *  - IKHSS6UPGR-9992 Spyder: SA - Contact's Phone Numbers in VIP Edit menu are barely visible due to gray text on a gray background
 *
 * v3.2.1.0.148 - Sreelakshmi
 *  - IKMAIN-44726: Accepted sample is shown as suggestion
 *
 * v3.2.1.0.150 - Aman
 *  - IKHSS7-34773 Addressing widget force close fix
 *
 * v3.2.1.0.151 - Rahul Vaish
 *  - IKHSS7-35371 resolution for illegalstateexception in action bar in calendar events
 *
 * v3.2.1.0.152 - Bhavana
 *  - IKCORE8-660 Turn off debug logs in Smart Actions
 *
 * v3.2.1.0.153 - Sreekanth
 *  - IKCORE8-695 Fix the issue due to which PAF was sending Publisher Modified intent to all Publishers on every restart
 *
 * v3.2.1.0.154 - Saral Kumar
 *  - IKCORE8-737 WiFi & BT static sensor based rules won't trigger after upgrade
 *
 * v3.2.1.0.155 - Rahul Vaish
 *  - IKHSS7-36468 Change the text color to white in events dialog
 *
 * v3.2.1.0.156 - Rahul Pal
 *  - IKINTNETAPP-592 UI Abstraction layer changes - Part 6
 *
 * v3.2.1.0.156 - Aman
 *  - IKHSS7-34634 Signature text changed for messages to be sent by Smart Actions
 *
 * v3.2.1.0.157 - Saral Kumar
 *  - IKINTNETAPP-614 Tag changes & interfaces according to RP doc
 *
 * v3.2.1.0.158 - Rahul Vaish
 *  - IKMAIN-43916 resolution for OutOfMemoryError in Wallpaper action
 *
 * v3.2.1.0.159 - Rahul Vaish
 *  - IKCORE8-500 header and footer view were not getting updated sometimes due to lack of focus
 *
 * v3.2.1.0.160 - Aman
 *  - IKHSS6UPGR-12310 Revert single segment SMS fix
 *
 * v3.2.1.0.161 - Yanyang Xie
 *  - IKHSS7-20048 BR:ET:HSS7:Vanquish: Selecting 'What is this' hyperlink shows
 *          'MotoActv' web page (Vanquish LE-25)
 *
 * v3.2.1.0.162 - Suren
 *  - IKINTNETAPP-625: Port the following changes from hss7 line.
 *      - IKHSS7-36295: The files folder should not have world writable permissions.
 *      - IKHSS7-35415: Fix force close seen in rules builder during monkey testing.
 *      - IKHSS7-35874: Change WiFi to Wi-Fi in strings.xml file.
 *      - IKHSS7-34996: Handle the 30 max visible rules issue in rules builder.
 *      - IKHSS7-36793: Update drive smart widget graphic.
 *      - IKHSS7-37250: Need to transition to Landing Page after adding a rule via all methods.
 *      - IKHSS7-37368: Motocare changes to log action name from the publisher key.
 *  - IKCORE8-968: Remove LED flag from Suggestion Notifications.
 *
 * v3.2.1.0.163 - Rahul Vaish
 *  - IKHSS6UPGR-12401 Database downgrade handling in Smart Actions
 *
 * v3.2.1.0.165 - Aman
 *  - IKMAIN-45502 Auto text reply database issue fix
 *
 * v3.2.1.0.166 - Chen Yan
 *  - IKHSS7-33192 PR: ET: HSS7 QNR/VNQ: "Tap" had better change to "Touch" for consistency, check CR IKHSS7-23640
 *
 * v3.2.1.0.167 - Sreelakshmi
 *  - IKMAIN-45670 Do not delete rules if refresh response is not received from action publishers
 *
 * v3.2.1.0.168 - Rahul Pal
 *  - IKINTNETAPP-592 UI Abstraction layer changes - Part 7 - Bug fixes
 *
 * v3.2.1.0.169 - Suren
 *  - IKINTNETAPP-622: DB changes for Battery Rules.
 *      - Database upgraded from V33 to V34. (12th June 2012)
 *      - Columns ParentRuleKey and AdopCount added to Rule table.
 *      - Column ChildRuleKey added to Action table.
 *      - Deprecated the use of SampleFkOrCount column in Rule table.
 *      - File fix done so that the adopted samples or suggestions visible to the user in
 *          the landing page will have the ParentRuleKey populated. For samples
 *          or suggestions shown in Add rule list page, the AdoptCount column is populated
 *          with the SampleFkOrCount column value.
 *
 * v3.2.1.0.170 - Aman
 *  - IKMAIN-45806 Play a playlist changes to select compatible players only
 *
 * v3.2.1.0.172 - Aman
 *  - IKMAIN-45931 Stop monitoring ringer volume when user changes mode or vibrate settings
 *
 * v3.2.1.0.173 - Suren
 *  - IKHSS7-38799: Change the padding in the buttons on the Landing Page welcome screen.
 *  - IKHSS7-38797: UI Enhancement as requested by CxD
 *      - Implement the new About dialog.
 *      - Change the rule icon dialog to show 3 items in a row.
 *      - Show Save/Cancel even for viewing a user visible landing page rule.
 *      - Update the initial suggestion dialog with new text and remove learn more button.
 *
 * v3.2.1.0.174 - Bhavana
 *  - IKHSS7-39158 Fix monkey crash in timeframes
 *
 * v3.2.1.0.175 - Aman
 *  - IKHSS7-39877 Play a playlist string change
 *
 * v3.2.1.0.177 - Sreelakshmi
 *  - IKHSS7-40124 The action publisher should not get any data when it is launched from the actions list
 *
 * v3.2.1.0.178 - Saral Kumar
 * - IKINTNETAPP-614 - Mandatory tag changes and response from Core to RP PUBLISH_RULE
 *
 * v3.2.1.0.179 - Yanyang Xie
 *  - IKCORE8-1092 PT:IT:Scorpion Mini I: 'Save' should be grayed when nothing selected in the required box.
 *
 * v3.2.1.0.180 - Yanyang Xie
 *  - IKCORE8-1475 CLONE - Smart Actions Suggested Driving Rule: Add GPS and Voice Announce to actions into Driving Rule
 *
 * v3.2.1.0.181 - Aman
 *  - IKINTNETAPP-618 Version number to be added to configurations of all actions
 *
 * v3.2.1.0.182 -haijin
 *  - IKHSS7-38263  invalid cell id check, only check -1 for GSM cell
 *
 * v3.2.1.0.184 - Aman
 *  - IKMAIN-43748 Brightness action to set automatic mode properly
 *
 * V3.2.1.0.185 - Rohit
 *  - IKMAIN-46543 - Rule blacklisting logic
 *
 * v3.2.1.0.186 - ChenYan
 * - IKHSS6UPGR-12687  PT:IT::Edison ICS LatAm:In portrait mode,Text cut off Smart Actions when font size is Huge and in Espanol
 *
 * v3.2.1.0.187 - Saral Kumar
 * - IKINTNETAPP-614 - Inferface change as per RP doc
 *
 * v3.2.1.0.188 - Aman
 *  - IKCORE8-3307 Ringer volume should revert properly
 *
 * v3.2.1.0.189 - Sreekanth
 *  - IKMAIN-45926 Fix for ANR caused due to querying Icon blob from DB on Main thread
 *
 * v3.2.2.0.0 - Carl Higashionna
 *  - IKMAIN-46586 Mirgration from Activity based UI to Fragment based UI.
 *
 * v3.2.2.0.1 - Aman
 *  - IKCORE8-3315 VIP Caller Mode to change volume when call comes
 *
 * v3.2.2.0.2 - Qinghu Li
 * - IKMAIN-46553:PR: ET: core9 spyder:New location details disappear when rotate phone .
 *
 * v3.2.2.0.3 - Qinghu Li
 * - IKMAIN-46719:Vanquish: Virtual keypad doesn't disappear pressing Home key from Smart Actions.
 *
 * v3.2.2.0.4 - Rahul Vaish
 * - IKMAIN-46863 added new string for device storage and changed phone calendar to device calendar
 *
 * v3.2.2.0.5 - Aman
 *  - IKCORE8-2981 Fix for raw numbers with special characters issue in chips UI
 *
 * v3.2.2.0.6 -haijin
 *  - IKCTXTAW-485 using wrapper for use case test in location sensor.
 *
 * v3.2.2.0.7 - Yanyang Xie
 *  - IKMAIN-45430 HSS7 SPD: Suggesiton is not added to the landing page.
 *
 * v3.2.2.0.8 - Rahul Vaish
 *  - IKHSS7-32769 Volumes action publisher implementation
 *
 * v3.2.2.0.9 - Aman
 *  - IKCORE8-3651 Fix force close in FrameworkUtils
 *
 * v3.2.2.0.10 - Suren
 *  - IKHSS7-42033: Rules importer changes related to Battery Saver DB columns.
 *  - IKMAIN-46959: Correct text displayed in initial suggestion dialog.
 *  - IKMAIN-46960: Changed Charging trigger text to be displayed.
 *  - IKMAIN-46958: Code changes to not persist adopted sample or copy rules before showing
 *      in the rules builder.
 *  - IKMAIN-46956: Fixed exception in Drive Widget DB code.
 *  - IKMAIN-46965: Changed sub-text displayed for Drive Smart sample.
 *  - IKMAIN-46964: Fixed exception in Drive Widget DB code.
 *  - IKMAIN-46961: Added defensive coding in Landing Page to fix FC seen in monkey testing.
 *  - IKMAIN-46957: Added defensive coding in Landing Page to fix FC seen in monkey testing.
 *
 * v3.2.2.0.11 - haijin
 *  - IKHSS7-42128 stop wifi scan when no movement where not currently in any POI
 *
 * v3.2.2.0.12 - Qinghu Li
 *  - IKMAIN-46720:Rule is diabled but all the acions are active permanently in smart actions in the following and someting related
 *
 * v3.2.2.0.13 - Yanyang Xie
 *  - IKCORE8-890 PT:IT:Scorpion Mini I:In portrait mode,Text cut off Smart Actions when font size is Huge and in Espanol.
 *  - IKMAIN-46475 PT:IT:Scorpion Mini I: Space should be added between the characters and cut-off line in VIP caller mode.
 *  - IKMAIN-46927 PR:ET:HSS7:SMq/VNQ: 'Motorola Location Services' need change to be 'Motorola location services' in Smart Actions settings
 *  - IKMAIN-46926 PR:ET:HSS7:QNR&VNQ: Rule status got stuck --will keep staying "Shutting down"/"Starting up" permanantly in the following in Smart Actions
 *
 * v3.2.2.0.14 - Qinghu Li
 *  - IKMAIN-46810:Repeated strict mode disk write violations are seen on contextual smartrules
 *  - IKHSS7-33218:Selected Locations comes to be Unselected in Trigger Locations in Smart Actions -- Only the first 8 locations got selected in the following
 *
 * v3.2.2.0.15 - Qinghu Li
 *  - IKMAIN-46924:Repeated Strict mode disk write violation is seen on contextual smart rules process
 *  - IKMAIN-47044:Repeated cursor leaks are seen on contextual smart rules when adding sample rules (S3-2)
 *
 * v3.2.2.0.16 - Aman
 *  - IKHSS7-42968 Remove unnecessary logs in Action receiver
 *
 * v3.2.2.0.17 - Vignesh karthik
 *  - IKMAIN-47319 Send revert_request when there is an in_progress response to fire_request
 *
 * v3.2.2.0.18 - Suren
 *  - IKHSS7-42596: None of the inferences apart from LBS are inferred.
 *
 * v3.2.2.0.19 - Haibin Li
 *  - IKMAIN-46666 Prop: IKHSS6UPGR-10008 : IKMAIN : TAM: EMEA: SPAIN: PROMPT: Truncated propmpts in Smart Actions suggestions.
 *
 * v3.2.2.0.20 - Rahul Vaish
 *  - IKMAIN-47117 - Changed Volumes icon to Ringer volume icon
 *
 * v3.2.2.0.21 - Bangalore Team
 * - IKCTXTAW-490 SmartActions : New Condition Publisher and Rule Publisher architecture and API
 *
 * v3.2.2.0.22 - Sreekanth
 * - IKCTXTAW-490 - Fix for showing Error for unavailable / blacklisted Condition publisher
 *
 * v3.2.2.0.23 - Rahul Vaish
 * - IKCTXTAW-490 - Multiple rules with Volumes action trigger on top of another were not reverting correctly
 *
 * v3.2.2.0.24 - Saral Kumar
 * - IKCTXTAW-490 - RI to prevent import of blacklisted RulePublisher
 *
 * v3.2.2.0.25 - Rahul Vaish
 * - IKCTXTAW-490 - Resolved more complicated cases in Volume AP
 *
 * v3.2.2.0.26 - Aman
 *  - IKHSS7-43520 Fix ANR in Ringtone action
 *
 * v3.2.2.0.27 - Aman
 *  - IKHSS7-44274 Modified contact to be reflected in puzzle builder after saving the rule
 *
 * v3.2.2.0.28 - Vignesh
 *  - IKHSS7-44274 Few fixes in mediator and Action.java for conflicting actions
 *
 * v3.2.2.0.29 - Rahul Pal
 *  - IKHSS7-40287 IKINTNETAPP-624 IKMAIN-45578
 *  Changes for Blacklist bug, greylist xml file implementation
 *  One time Loc Wi-Fi dialog to be shown oly in case of Autoscan not supported
 *  Dont show h/w product dependent Publishers such as Loc and Processor speed if they
 *  are not supported.
 *
 * v3.2.2.0.30 - Bhavana
 *  - IKCTXTAW-490 Fix timeframes crash
 *
 * v3.2.2.0.32 - Sreelakshmi
 * - IKCTXTAW-490 Klocwork fixes, minor fixes in Action Persistence
 *
 * v3.2.2.0.33 - Saral Kumar
 * - IKCTXTAW-490 - For a suggestion, if blocks are unconfigured launch RulesBuilder
 *
 * v3.2.2.0.35 - Saral Kumar
 * IKCTXTAW-490 - Make Suggestion Init dialog same as hss7
 *
 * v3.2.2.0.37 - Sreelakshmi
 * - IKCTXTAW-490 SA core issues reported in testing, Review comments
 *
 * v3.2.2.0.38 - Aman
 *  - IKCTXTAW-490 Processor Speed to be removed for old framework apks
 *
 * v3.2.2.0.39 - Saral Kumar
 * IKCTXTAW-490 - Support for Green status indication for new suggested Condition blocks
 *
 * v3.2.2.0.40 - Aman
 *  - IKCTXTAW-490 Mediator review comments incorporated
 *
 * v3.2.2.0.41 - Yanyang Xie
 *  - IKHSS7-44159 PR:ET:HSS7:SMq: "Done" comes to be enabled even when no locations checked in Smart Actions ->Trigger Location
 *
 * v3.2.2.0.42 - Sreelakshmi
 * - IKCTXTAW-490 SA core issues reported in testing, Findbugs and lint fixes,
 *   Delete blacklisted or uninstalled (and without a valid market url) publisher blocks from suggestions/samples
 *
 * v3.2.2.0.44 - Rahul Vaish
 * - IKMAIN-47074 changed alarm icon in Volumes AP
 *
 * v3.2.2.0.46 - Bhavana
 * - IKCTXTAW-490 Missed call config fix
 *
 * v3.2.2.0.46 - Jun Li
 * - IKMAIN-46996: PR:ET:HSS7:SMq: Couple issues in Smart Actions Learn more text almost get outspilled in Smart Action.
 * - IKCORE8-5106: PT:IT:Scorpion Mini I:In portrait mode,Text cut off Smart Actions when font size is Huge and in Espanol.
 *
 * v3.2.2.0.47 - Sreelakshmi
 * - IKCTXTAW-490 SA core issues reported in testing, Review comments
 *
 * v3.2.2.0.50 - Aman
 *  - IKCTXTAW-490 Mediator should always allow subscribe requests to pass
 *
 * v3.2.2.0.51 - Sreekanth
 *  - IKCTXTAW-490 iChanges to show Rule Icons from Icon table, this allows to show icons from 2nd and 3rd party published Rules
 *
 * v3.2.2.0.52 - Bhavana
 *  - IKCTXTAW-490 CP Review comments incorporation
 *
 * v3.2.2.0.53 - Chen Yan
 * - IKMAIN-46287 PR: ET: HSS9 SPD: Icons show in highlighted and gray-out way randomly in Actions after edit with no save
 * - IKMAIN-45568 PR: ET: HSS9: Bluetooth device display wrongly in Smart actions after set driver smart.
 * - IKCORE8-4696 Double quotes must not be present in Smart Actions reminders
 * - IKMAIN-47715 disconnected action bar don't have consistent UI
 *
 * v3.2.2.0.54 - Sreelakshmi
 * - IKCTXTAW-490 SA core issues reported in testing, Changes for MMDB Cleanup
 *
 * v3.2.2.0.55 - Rahul Vaish
 * - IKCTXTAW-490 RTC for inference, review comments, and other fixes
 *
 * v3.2.2.0.56 - Aman
 *  - IKCTXTAW-490 Persistence changes for APs
 *
 * v3.2.2.0.57 - Saral Kumar
 * -  IKCTXTAW-490 - Removing deprecated API's for Suggestions.
 *
 * v3.2.2.0.58 - Aman
 *  - IKCTXTAW-490 Brightness UI fix and mediator refresh response change
 *
 * v3.2.2.0.59 - Saral Kumar
 * -  IKCTXTAW-490 - KW fixes for RI.
 *
 * v3.2.2.0.60 - Bhavana
 * -  IKCTXTAW-490 - Minor review comments.
 *
 * v3.2.2.0.61 - Sreekanth
 * -  IKCTXTAW-490 - Fix fot background sync issue and fix for stopping RuleValidatorService
 *
 * v3.2.2.0.62 - Rahul Vaish
 * - IKCTXTAW-490 implemented dynamic receiver in wifi connection cp
 *
 * v3.2.2.0.63 - Jun Li
 * - IKMAIN-46996: PR:ET:HSS7:SMq: Couple issues in Smart Actions ----"Learn more" text almost get outspilled in Smart Actions
 *
 * v3.2.2.0.64 - Sreelakshmi
 * - IKCTXTAW-490 SA core issues reported in testing
 *
 * v3.2.2.0.65 - Aman
 *  - IKCTXTAW-490 Mediator to allow refresh response with null config to pass
 *
 * v3.2.2.0.68 - Rahul Vaish
 * - IKMAIN-48134 resolved the problem of duplicate calendars
 *
 * v3.2.2.0.69 - Bhavana
 * - IKCTXTAW-490 CP Clean up
 *
 * v3.2.2.0.70 - Saral Kumar
 * - IKCTXTAW-490 - PublisherUiController changes for system dialogs
 *
 * v3.2.2.0.71 - Sreelakshmi
 * - IKCTXTAW-490 Review comments incorporation and SA core issues reported in testing
 *
 * v3.2.2.0.72 - Rahul Vaish
 * - IKCTXTAW-490 user trial issues, reported issues fixes
 *
 * v3.2.2.0.73 - Aman
 *  - IKCTXTAW-490 Fix GPS revert issue
 *
 * v3.2.2.0.74 - Boby Iyer
 *  - IKMAIN-48216 : Fix the name clash of Child rules for actions
 *
 *  v3.2.2.0.75 - Sreekanth
 *  - IKCTXTAW-490  - Implementation of subscribe retry if subscribe response is not received
 *
 * v3.2.2.0.77 - Rahul Vaish
 * - IKCTXTAW-490 molded Volumes AP to mimic Ringer volume behavior for settings change
 *
 * v3.2.2.0.78 - Aman
 *  - IKHSS6UPGR-14866 Avoid FC in Wifi due to unacquired permission
 *
 * v3.2.2.0.79 - Vignesh
 *  - IKCTXTAW-490 Permissions for AP-CP-RP
 *
 * v3.2.2.0.80 - Sreekanth
 * - IKCTXTAW-490 - Rules Validator support to ignore unavailable / blacklisted publishers and RulesBuilder
 *                  changes not to show invalid publishers
 *
 * v3.2.2.0.81 - Haijin
 * - IKHSS7-45876 empty scans debounce and hold wakelock to avoid zero scan result.
 *
 * v3.2.2.0.82 - Sreekanth
 * - IKCTXTAW-490 - Fix FC after Clear data
 *
 * v3.2.2.0.83 - Saral Kumar
 * - IKCTXTAW-490 - Green light indication is not shown for the suggested condition
 *
 * v3.2.2.0.84 - Sreekanth
 * - IKCTXTAW-490 - Fix for Manual Rule
 *
 * 3.2.2.0.85 - Boby Iyer
 * - IKMAIN-48371 : prop IKHSS7-46082 : refresh command should update config with current supported options
 *
 * 3.2.2.0.87 - Vignesh
 * - IKCTXTAW-490 : Correct permissions from Mediator
 *                  Avoid Permission denial error when there is an adapter for Motion CP
 *
 * 3.2.2.0.90 - Aman
 *  - IKHSS7-46639 Screen timeout fix for user change event.
 *
 *  3.2.2.0.92 - Sreekanth
 *  - IKCTXTAW-490 : Change in the timeout for Publishers Refresh Response and fix for Rules showing Error for small time when newly created
 *
 *  3.2.2.0.93 - Boby Iyer
 *  - IKMAIN-48500 : Prop IKHSS7-45828 : Active Child Rule should be deleted via SmartRulesService
 *
 * 3.2.2.0.94 - Rahul Vaish
 * - IKCTXTAW-490 support for multiple inferences with option field in config
 *
 * 3.2.2.0.95 - Aman
 *  - IKHSS7-46814 Hide bookmarks bar if no bookmarks are present in the database
 *
 * v3.2.2.0.96 - Saral Kumar
 * - IKCTXTAW-490 - Support for Manul Consent dialog for blacklisted connected triggers
 *                  Configuration text to be shown in the aciton description
 *
 * v3.2.2.0.97 - Saral Kumar
 * - IKCTXTAW-490 - Reducing logs in RI
 *
 * v3.2.2.0.98 - Bhavana
 * - IKCTXTAW-490 - Log opt in CP
 *
 * v3.2.2.0.99 - Saral Kumar
 * - IKCTXTAW-490 - During Sample update the adopt count should be updated
 *
 * v3.2.2.0.96 - Sreelakshmi
 * - IKCTXTAW-490 - Correct the strings to show 'About' menu option. Fix issues in displaying location blocks.
 *
 * v3.2.2.0.101 - Rahul Vaish
 * - IKJBREL1-460 resolved FC happening due to null ssid
 *
 * v3.2.2.0.102 - Rahul Vaish
 * - IKJBREL1-486 resolved FC due to a rare race condition
 *
 * v3.2.2.0.103 - Bhavana
 * - IKCTXTAW-490 Refresh response to send SUCCESS if config null
 *
 * v3.2.2.0.104 - Rahul Vaish
 * - IKJBREL1-381 resolved problems in WiFi connection due to null and empty ssid
 *
 * v3.2.2.0.105 - Saral Kumar
 * - IKCTXTAW-490 Update the adopt count when a suggestion is accepted from sample
 *
 * v4.2.2.0.106 - Boby Iyer
 * - IKMAIN-48648 Prop: IKHSS7-33423 : Reorder Drive Mode actions for KPI
 * - Also fix the version number
 *
 * v3.2.2.0.107 - Sreekanth
 * - IKCTXTAW-490 - Support for converting Rules with only blacklisted / unavailable or disabled condition publisher as Manual rule
 *
 * v3.2.2.0.108 - Saral Kumar
 * - IKCTXTAW-490 - After the block is configured the description text should be in gray color.
 *
 * v3.2.2.0.109 - Bhavana
 * - IKJBREL1-571 Crash in missed call UI.
 *
 * v3.2.2.0.110 - Aman
 *  - IKJBREL1-639 Fix crash in chips UI
 *
 * v3.2.2.0.112 - Saral Kumar
 * - IKCTXTAW-490 - Save button disabled for blacklisted condition.
 *
 * v3.2.2.0.113 - Bhavana
 * - IKCTXTAW-490 - BT refresh response to send null config.
 *
 * v3.2.2.0.114 - Rahul Vaish
 * - IKCTXTAW-490 device calendar is not present in JB
 *
 * v3.2.2.0.115 - Aman
 *  - IKCTXTAW-490 Remove visible group check from chips
 *
 * v3.2.2.0.116 - Aman
 *  - IKCTXTAW-490 Change launch website error message
 *
 * v3.2.2.0.120 - Vignesh
 *  - IKCTXTAW-490 Better handling of MEDIATOR_INIT when there is high traffic
 *
 * v3.2.2.0.121 - Carl
 *  - IKMAIN-46586 Updating cloned Activities to the latest version for code review purposes.
 *
 * v3.2.2.0.122 - Carl
 *  - IKMAIN-46586 Core changes to migrate from Activity based screens to Fragment based.
 *
 * v3.2.2.0.123 - Aman
 *  - IKCTXTAW-503 Logging changes for Action Publishers
 *
 * v3.2.2.0.124 - Sreekanth
 *  - IKJBREL1-591 : isValidPublisher API for checking whether the given publisher is available / blacklisted
 *
 * v3.2.2.0.125 - Sreelakshmi
 *  - IKJBREL1-1027 Invoke rules validator after blacklist file is removed
 *
 * v3.2.2.0.126 - Yanyang Xie
 *  - IKJBREL1-1005 Prop: IKCTXTAW-504 : IKJBREL1 : Make SmartActions UI changes per the new requirements from CxiD -- Merge Suren's changes first.
 *
 * v3.2.2.0.127 - Bhavana
 *  - IKCTXTAW-503 Missed call fix
 *
 * v3.2.2.0.128 - Saral Kumar
 * - IKCTXTAW-503 - Prevention of profile restore from 2.2 to 2.1
 *
 * v3.2.2.0.129 - Qinghu Li
 *  - IKJBREL1-1136:Rule stays in "Starting up" permanantly when change Rule icon and then Turn it ON
 *  - IKJBREL1-573:Soft input not showing up when choose "selected contacts" in Action-Auto reply text in Smart Actions
 *
 * v3.2.2.0.130 - Vignesh karthik
 *  - IKCTXTAW-503 Fix to ignore unavailable publishers in the suggestion count
 *
 *  v4.2.2.0.131 - Boby Iyer
 *  - IKHSS7-47713 : Upgrade issue for Background data
 *
 *  v4.2.2.0.132 - Bhavana
 *  - IKCTXTAW-503 : Fix Voice Announce conflict issue
 *
 *  v4.2.2.0.132 - Chen Yan
 *  - IKJBREL1-825 PR:ET:JB:SMq: No days selected but the "Save" botton is enabled and It shows as "Never" When Add a timeframe in Smart Actions
 *
 * v4.2.2.0.133 - Aman
 *  - IKCTXTAW-503 Fix location force close
 *
 * v4.2.2.0.135 - Rahul Vaish
 * - IKMAIN-48790 added support for volumes percentages and custom vibrate settings
 *
 *  v4.2.2.0.136 - Boby Iyer
 *  - IKHSS7-47988 : Backup restore needs the same fix as IKHSS7-47713
 *  - IKCTXTAW-532 : Resource update
 *
 * v4.2.2.0.137 - Aman
 *  - IKCTXTAW-503 Fix wallpaper force close
 *
 * v4.2.2.0.138 - Sreelakshmi
 *  - IKCTXTAW-503
 *   - Processor speed is listed in Battery saver rule without description
 *   - After a locale change, manual rules are disabled
 *
 * v4.2.2.0.140 - Rahul Vaish
 * - IKMAIN-48790 JB specific changes in Volumes
 *
 * v4.2.2.0.142 - Saral Kumar
 * - IKCTXTAW-503 -  Suggestion implementation
 *
 * v4.2.2.0.143 - Sreelakshmi
 * - IKCTXTAW-503 - Remove Context Engine's aidl file
 *
 * v4.2.2.0.144 - Sreekanth
 * - IKCTXTAW-503 - Changed the logic to store IconBlob in Rule Instance instead of Icon Drawable
 *
 * v4.2.2.0.145 - Carl
 * - IKCTXTAW-538 Modifications to handle delete from pickers. Sync Fragments. Handle null string in block class.
 *
 * v4.2.2.0.146 - Aman
 *  - IKCTXTAW-503 Config validation for APs
 *
 * v4.2.2.0.147/148 - Ghouse
 *  - IKCTXTAW-536 Added support for Data Analytics to log Settings information as normal checkin and also part of bootup/weekly support too.
 *
 * v4.2.2.0.149 - Rahul Vaish
 * - IKMAIN-48790 handled upgrade from ics to jb in Volumes AP
 *
 * v4.2.2.0.150 - Boby Iyer
 * - IKJBREL1-586 : Send Condition Publisher intent after rules import
 *
 * v4.2.2.0.151 - Ghouse
 * - IKJBREL1-1159 : Force close in rulesbuilder seen during monkey test
 * - IKJBREL1-1073 : Smart Actions: Launch Website is seen graylisted.
 * - IKJBREL1-1061 : PuzzleBuilder appears again
 *
 * v4.2.2.0.152 - Ivan/Ghouse
 * - IKJBREL1-2177 : Guided Picker change.
 *
 * v4.2.2.0.153 - Carl
 * - IKCTXTAW-541 : Switch to 2.2 UI.
 *
 * v4.2.2.0.154 - Ghouse
 * - IKJBREL1-2844 : Text is not readable on suggestion card.
 *
 * v4.2.2.0.155 - Ivan
 * - IKJBREL1-2847 : Updated the BT Connection picker to use the new 2.2 architecture.
 *
 * v4.2.2.0.156 - Carl
 * - IKCTXTAW-547 Switch back to 2.1 Rule Editor UI.
 *
 * v4.2.2.0.156 - Ivan
 * - IKJBREL1-3334 : Added support for html format tags in the block descriptions.
 *
 * v4.2.2.0.160 - Alan Sien Wei Hshieh
 * - IKJBREL1-3148 Smart Actions Force close {java.lang.IndexOutOfBoundsException: Invalid index 0, size is 0,at java.util.ArrayList.throwIndexOutOfBoundsException(ArrayList.java:251)}
 *
 * v4.2.2.0.161 - Alan Sien Wei Hshieh
 * - IKJBREL1-3463 [Smart Actions][SMQ] - (Cherrypick)The text appears truncated when fonts are huge
 *
 * v4.2.2.0.162 - Alan Sien Wei Hshieh
 * - IKCORE8-8285 (Cherrypick) SmartActions vip ringer input field doesnt allow space B2GID:1744127
 *
 * v4.2.2.0.163 - Boby Iyer
 * IKJBREL1-2840 : ANR - Move RulesValidatorService to lower priority thread.
 *
 * v4.2.2.0.164 - Ivan Wong
 * IKJBREL1-3324 : Updated the hardcoded path to the BT connection picker.
 *
 * v4.2.2.0.165 - Ivan Wong
 * IKJBREL1-2843 : Fixed the error checking code for the timeframe and location picker when a timeframe or location is deleted.
 *
 * v4.2.2.0.166 - Alan Sien Wei Hshieh
 * IKJBREL1-3401 Ringtone Vibrate option is not saved
 * IKJBREL1-3191 User should not be allowed to select maximum ringtone and silent mode when configure a ringer volume trigger
 *
 * v4.2.2.0.167 - Craig Detter
 * IKJBREL1-3962 java.lang.ClassNotFoundException: Didn't find class "com.motorola.contextual.smartprofile.sensors.calendareventsensor.AgendaItemView
 *
 * v4.2.2.0.168 - Ivan Wong
 * IKJBREL1-3321 : Updated the basic Ringer picker to properly interface with the Volumes publisher.
 *
 * v4.2.2.0.169 - Ivan Wong
 * IKJBREL1-2860 : This is the first step to update the VipRinger picker to match the publisher interface.
 *
 * v4.2.2.0.170 - Boby Iyer
 * IKJBREL1-2921 : Clear RP version Shared Preference if upgrading from 2.1
 *
 * v4.2.2.0.171 - Carl Higashionna
 * IKJBREL1-3773 : Fixed DialogFragment instantiation in EditLocationActivity.
 *
 * v4.2.2.0.172 - Carl Higashionna
 * IKJBREL1-3787 : Applied Alan's fixes for DialogFragment instantiation in WelcomeScreenActivity.
 *
 * v4.2.2.0.173 - Alan Sien Wei Hshieh
 * IKJBREL1-3933 SA: Rule is not kicking in if the Trigger is Any Wifi Network
 *
 * v4.2.2.0.174 - Craig Detter
 * IKJBREL1-2806 : Added logging for music player, cleaned up several small code issues.
 *
 * v4.2.2.0.175 - Craig Detter
 * IKJBREL1-2806 : 2 of 2 - Added more logging for music player error block issue.
 *
 * v4.2.2.0.176 - Ghouse Adoni
 * IKJBREL1-4323 : Correct the Motion Detector string.
 *
 * v4.2.2.0.177 - Alan Sien Wei Hshieh
 * IKCTXTAW-549 PR: ET: SMQ: Cursor get cropped and added contact info cannot display if soft keyboard popup in landscape mode in Smart Actions
 *
 * v4.2.2.0.178 - Craig Detter
 * IKJBREL1-4324 : Fix 2 different ANR root causes.
 *
 * v4.2.2.0.179 - Boby Iyer
 * IKJBREL1-4188 : Background Sync issues and cleanup
 *
 * v4.2.2.0.180 - Alan Sien Wei Hshieh
 * IKJBREL1-3877 I'm done" came to disable but options got checked when rotating device, user need click the seleted options again to let the "I'm done" botton light up
 *
 * v4.2.2.0.181 - Boby Iyer
 * IKJBREL1-4289 : Update class names for STATE Monitor after guided picker changes
 *
 * v4.2.2.0.182 - Ghouse Adoni
 * IKJBREL1-4567 : Updating the SDK version and cleaning the actions to point to correct picker class.
 *
 * v4.2.2.0.183 - Alan Sien Wei Hshieh
 * IKJBREL1-3800 : PR:ET:JB:SMq:Smart Actions Force close {java.lang.IllegalStateException: The specified child already has a parent. You must call removeView() on the child's parent first.}
 * IKJBREL1-4145 : PR: ET: SMQ: Checked charging status become unchecked in smart actions after rotating phone's screen.
 * IKJBREL1-4397 "Incoming" is missing in Action "Announce calls and texts"
 *
 * v4.2.2.0.184 - Carl Higashionna
 * IKCTXTAW-551 : ContactsChooserFragment force close during monkey test. Caused by access to host activity resources from onCreateView method.
 *
 * v4.2.2.0.185 - Carl Higashionna
 * IKJBREL1-2856 : Removed bookmarks from picker since Chrome on JB no longer exposes the bookmarks api.
 *
 * v4.2.2.0.186 - Alan Sien Wei Hshieh
 * IKJBREL1-4458 : Smart Actions: Selecting Any Wi-Fi Network is not disabling all other available networks in the list.
 *
 * v4.2.2.0.187 - Craig Detter
 * IKJBREL1-4594 : ANR fixes in RingtoneActivity and Ringtone were making calls directly to Db query on UI thread
 *
 * v4.2.2.0.188 - Carl Higashionna
 * IKJBREL1-4639 : 2nd attempt to fix ContactsChooserFragment force close during monkey test.
 *
 * v4.2.2.0.189 - Alan Sien Wei Hshieh
 * IKJBREL1-4458 : Smart Actions: Selecting Any Wi-Fi Network is not disabling all other available networks in the list.
 *
 * v4.2.2.0.190 - Craig Detter
 * IKJBREL1-4732 fix on ANR fix IKJBREL1-4594 : index out of bounds exception, AsyncTask, doInBackground() not invoked with parameters.
 *
 * v4.2.2.0.191 - Ivan Wong
 * IKJBREL1-4611 Fixed the display string in calendar.
 * IKJBREL1-4039 Marked strings as "not translatable".
 *
 * v4.2.2.0.192 - Boby Iyer
 * IKJBREL1-4770 : Tweak Media Player release calls to fix FC
 *
 * v4.2.2.0.193 - Carl Higashionna
 * IKJBREL1-4649 : EditLocationNameDialogFragment force close issue resolved.
 *
 * v4.2.2.0.194 - Craig Detter
 * IKJBREL1-4220 : ANR LandingPageActivity - icon fetch being done on UI thread - during monkey test
 *
 * v4.2.2.0.195 - Boby Iyer
 * IKJBREL1-4220 : Another ANR fix, Lower the priority of Handler Thread in RulesValidatorService
 *
 * v4.2.2.0.196 - Carl Higashionna
 * IKJBREL1-4830 : 3rd attempt to fix ContactsChooserFragment force close during monkey test.
 *
 * v4.2.2.0.197 - Ghouse Adoni
 * IKJBREL1-4755 : Suppress Excessive Debug logs - phase 1
 *
 * v4.2.2.0.198 - Boby Iyer
 * IKJBREL1-4905 : Need to validate rule before refresh_response is processed.
 *
 * v4.2.2.0.199 - Ivan Wong
 * IKJBREL1-4941 : Updated RingtoneActivity to use AsyncTasks instead of work thread.
 *
 * v4.2.2.0.200 - Ghouse Adoni
 * IKJBREL1-4755 : Suppress Excessive Debug logs - final change.
 *
 * v4.2.2.0.201 - Ghouse Adoni
 * IKJBREL1-5244 : Changing SmartActions to be one work.
 *
 * v4.2.2.0.202 - Boby Iyer
 * IKJBREL1-5293 : Tweak Media Player release calls to (again) fix FC
 *
 * v4.2.2.0.203 - Ghouse Adoni
 * IKJBREL1-5286 : Change name from Drive Smart to Drive
 *
 * v4.2.2.0.204 - Ghouse Adoni
 * IKJBREL1-4305/IKJBREL1-5274 : Disable Verbose logging in Location Sensor and fix the missed call use case.
 *
 * v4.2.2.0.205 - Alan Sien Wei Hshieh
 * IKJBREL1-5289 JB:UT: Cutoff words are seen in smart actions while creating rule
 *
 * v4.2.2.0.206 - Ghouse Adoni
 * IKJBREL1-5536 : Fix the Database leak potential issue.
 *
 * v4.2.2.0.207 - Boby Iyer
 * IKCTXTAW-580 : Link adopted sample rules to parent sample rule in import
 *
 * v4.2.2.0.208 - Alan Sien Wei Hshieh
 * IKJBREL1-5647 Smart Actions: Unable to Add contact in Send Text Message
 *
 * v4.2.2.0.209 - Ivan Wong
 * IKJBREL1-5646 : Put onCreateView in LaunchAppPickerFragment in an AsyncTask
 *
 * v4.2.2.0.210 - Alan Sien Wei Hshieh
 * IKJBREL1-5727 JB:UT: SmartActions introduction screen does not scroll while font size is set to large
 *
 * v4.2.2.0.211 - Ivan Wong
 * IKCORE8-7641 : Propagated the help screen localization bug to JB.
 *
 * v4.2.2.0.212 - Boby Iyer
 * IKJBREL1-6262 : Fix Options for LBS and Battery Saver
 *
 * v4.2.2.0.213 - Ghouse Adoni
 * IKCTXTAW-587 IKCTXTAW-588: Fixed Motocare logging which got broke due to new enhancements.
 *
 * v4.2.2.0.214 - Ghouse Adoni
 * IKJBREL1-6240 : Wifi Disconnect issue fix.
 *
 * v4.2.2.0.215 - Boby Iyer
 * IKJBREL1-6520 : JB Ringer Vibrate setting change
 *
 * v4.2.2.0.216 - Ghouse Adoni
 * IKMAINJB-189 : Catch an Sql Full exception during end transaction.
 *
 * 4.2.2.0.217 - Alan Sien Wei Hshieh
 * IKJBREL1-6213 PR:SMQ: Too many blank options in location name after change locations in SmartActions.
 *
 * 4.2.2.0.218 - Carl Higashionna
 * IKJBREL1-6875 : Fixed force close is seen in LaunchAppPickerFragment performing Smart Actions monkey test.
 *
 * 4.2.2.0.219 - Alan Sien Wei Hshieh
 * IKMAINJB-353 PT:PR:VanquishLatamJB:SmartAction force close when launch it from recent app after change language
 * (Part one)
 *
 * 4.2.2.0.220 - Alan Sien Wei Hshieh
 * IKMAINJB-601 Repair picker fragments (2)
 *
 * 4.2.2.0.221 - Carl Higashionna
 * IKJBREL1-6911 Async task added to LandingPageActivity to call RulePersistence.fetchFullRule()
 *
 * v4.2.2.0.222 - Boby Iyer
 * IKJBREL1-6880 : ANR fix : Remove repeated calls to RulesValidatorService
 *
 * v4.2.3.0.1 - Ghouse Adoni
 * IKJBREL1-7058: Launch URl action has no more affect on Dock trigger.
 *
 * v4.2.3.0.2 - Alan Sien Wei Hshieh
 * IKJBREL1-6645 Smart Actions: Soft input keyborad is not automatically poping in
 *
 * v4.2.3.0.3 - Craig Detter
 * IKCTXTAW-596 : Refactor psf
 *
 * v4.2.3.0.4 - Ivan Wong
 * IKCTXTAW-595 : LaunchWebsite picker should use the url string if the title of site is empty.
 *
 * v4.2.3.0.5 - Carl Higashionna
 * IKJBREL1-7297 : Added async task to LaunchWebsite picker to fix ANR.
 *
 * v4.2.3.0.6 - Ivan Wong
 * IKJBREL1-7247 : Fixed to correctly set the default for the reminder picker.
 *
 * v4.2.3.0.7 - Carl Higashionna
 * IKJBREL1-7391 : Fixed Ringtone Chooser to handle device rotation.
 *
 * v4.2.3.0.8 - Ghouse Adoni
 * IKCTXTAW-608 - Enable SmartActions to be backward compatible to ICS.
 *
 * v4.2.2.1.9 - Ghouse Adoni
 * IKCTXTAW-610 - Anr fix in SettingsReceiver
 *
 * v4.2.2.1.10 - Carl Higashionna
 * IKMAINJB-773 : Fix fragment constructor in PlayMusic picker which was causing force close.
 *
 * v4.2.2.1.11 - Boby Iyer
 * IKJBREL1-7748 : Additional checks for wifi state
 *
 * v4.2.2.1.12 - Ghouse Adoni
 * IKCTXTAW-619: Correct Resource for Vanquish and Dinara
 *
 * v4.2.2.1.13 - Boby Iyer
 * IKJBREL1-8025 : Ringer Mode changes
 *
 * v4.2.2.1.14 - Ghouse Adoni
 * IKJBREL1-7752: Workaround for Google TTS issue.
 *
 * v4.2.2.1.15 - Craig Detter
 * IKJBREL1-8126: Out of Memory Issue.
 *
 * v4.2.2.1.16 - Ivan Wong
 * IKJBREL1-7925: Fixed a NullPointerException in the EditLocationActivity.
 *
 * v4.2.2.1.17 - Ghouse Adoni
 * IKJBREL1-7928: Fix NPE due to too many clicks.
 *
 * v4.2.2.1.18 - Ghouse Adoni
 * IKMAINJB-1377: Fix the ANR while accessing or deleting the rule list
 *
 * v4.2.2.1.19 - Ghouse Adoni
 * IKMAINJB-1377: DB delete operation in async task.
 *
 * v4.2.2.1.19 - Alan Sien Wei Hshieh
 * IKCTXTAW-634 Picker fragment refactoring (Location condition)
 *
 * v4.2.2.1.20 - Boby Iyer
 * IKMAINJB-1642 : ANR fix; refactor EditRuleActivity
 *
 * v4.2.2.1.21 - Boby Iyer
 * IKMAINJB-1953 : Destroy suggestion activity when back is pressed.
 *
 * v4.2.2.1.22 - Alan Sien Wei Hshieh
 * IKCTXTAW-634 Picker fragment refactoring (Calendar and Time Frame)
 *
 * v4.2.2.1.23 - Carl Higashionna
 * IKCTXTAW-634 Picker fragment refactoring
 * Action Pickers: Send Text, Reminder, Play Music, Ringtone, Wallpaper,
 * Launch App, Launch Website, Auto Reply Text, VIP Ringer, Brightness, Background Data
 * Condition Pickers: Missed Call
 *
 * v4.2.2.1.24 - Boby Iyer
 * IKCTXTAW-654 : Add loading dialog for drive mode
 *
 * v4.2.2.1.25 - Boby Iyer
 * IKCTXTAW-590 : add function to check if POI can be deleted
 *
 *  v4.2.2.1.26 - Navin Dabhi
 * IKCTXTAW-567 IKCTXTAW-583 IKCTXTAW-577 IKCTXTAW-575 IKCTXTAW-572 IKCTXTAW-565
 * IKCTXTAW-571 IKCTXTAW-570 IKCTXTAW-569 IKCTXTAW-568 IKCTXTAW-566 IKCTXTAW-564
 * IKCTXTAW-562 IKCTXTAW-561 IKCTXTAW-559 IKCTXTAW-558 IKCTXTAW-557 IKCTXTAW-556
 * IKCTXTAW-555 IKCTXTAW-553 IKCTXTAW-552 : String changes based on new CxD spec
 *
 * v4.2.2.1.27 - Alan Sien Wei Hshieh
 * IKCTXTAW-616 The "Please fill all the details" toast miss "." symbol at last.
 *
 * v4.2.2.1.28 - Sunnyvale team
 * IKMAINJB-2366 Fix java.lang.IllegalStateException: Activity has been destroyed in Calendar Activity.
 * IKCTXTAW-554 The text on button should be in upper case on What is SMART ACTIONS? screen.
 * IKCTXTAW-639 First letter for adding URL should not be capital in SmartActions
 * IKCTXTAW-578 [Smart Action][UI] - The toast used  is totally different from CXD. The toast is missing in certain cases.
 * IKCTXTAW-581 Smart Actions: "Timeframes" string is still shown in My Profile of Smart Actions
 * IKCTXTAW-563 [Smart Actions][UI] - About Smart Actions window should have a Cancel button instead of OK.
 *
 * v4.2.2.1.29 - Boby Iyer
 * IKCTXTAW-656 : Handle VIP Ringer before Voice Announce
 *
 * v4.2.2.1.30 - Boby Iyer
 * IKCTXTAW-656  : Caller ID repeat for voice announce calls
 *
 * v4.2.2.1.31 - Ghouse Adoni
 * IKMAINJB-2970 : To fix the NPE in Edit rule activity.
 *
 * v4.2.2.1.32 - Carl Higashionna
 * IKMAINJB-3047 Location Picker fix. Reverting gerrit change id: 484685.
 *
 * v4.2.2.1.33 - Navin Debhi
 * IKCTXTAW-674 [SmartActions] [UI] - Home Rule should be just Home.
 * IKCTXTAW-617 Scroll bar is overlap with frame line in Smart Actions->Sleep rule->Timeframe.
 *
 * v4.2.2.1.34 - Carl Higashionna
 * IKMAINJB-3382 Error not displayed when in-use location is deleted from location list.
 *
 * v4.2.2.1.35 - Ghouse Adoni
 * IKMAINJB-3427 : To fix the force close issue caused due to orientation in EditRuleActivity
 *
 * v4.2.2.1.36 - Carl Higashionna
 * IKCTXTAW-689 - Location picker and My Profile locations fixes as per CxD specification.
 *
 * v4.2.2.1.37 - Ghouse Adoni
 * IKCTXTAW-705: The min SDK version is corrected to ICS version.
 *
 * v4.2.2.1.38 - Carl Higashionna
 * IKMAINJB-3532 : Added default selection to first screen of Auto reply text action.
 *
 * v4.2.2.1.39 - Carl Higashionna
 * IKMAINJB-3755 : Removed Lapdock option per CxDs recommendation.
 *
 * v4.2.2.1.40 - Ghouse Adoni
 * IKJBREL1-9251: The Wifi trigger and Cellular Data is conflicting with the framework, which does it automatically and hence SmartAction
 *                use case is invalid. Added Cellular Data action to be grayed out in case Wifi trigger is selected and Vice versa.
 *
 * v4.2.2.1.41 - Carl Higashionna
 * IKMAINJB-3405 : Added hilight to typeTWO and typeTHREE picker list items in CustomListAdapter.
 *
 * v4.2.2.1.42 - Boby Iyer
 * IKMAINJB-2756 : Update Rules Validation after edit
 *
 * v4.2.2.1.43 - Carl Higashionna
 * IKMAINJB-3761 : Fix force close in EditRuleActivity. Moved initialization of content view prior to async task used to load rule.
 *
 * v4.2.2.1.44 - Carl Higashionna
 * IKMAINJB-4089 : Fixed WifiConnectionChooserFragment picker fragment contructor.
 *
 * v4.2.2.1.45 - Ghouse Adoni
 * IKMAIN-49262 : Fix - Highlighted Icon should be in Blue.
 *
 * v4.2.2.1.46 - Boby Iyer
 * IKMAINJB-3856 : Check for Max Number of rules from Drive Mode
 *
 * v4.2.2.1.47 - Carl Higashionna
 * IKMAINJB-3995 : AddressUtil incorrectly setting knownFlags property to 1 on subsequent call.
 *
 * v4.2.2.1.48 - Vel Pratheesh Sankar
 * IKJBREL1-8354 : Phone thinks my location is at home when I am at work.
 *
 * v4.2.2.1.49 - Carl Higashionna
 * IKMAINJB-4485 : Reverting location_consent value in strings.xml back to original location consent message.
 *
 * v4.2.2.1.50 - Vel Pratheesh Sankar
 * IKMAINJB-2844 : Backup and restore issue
 *
 * v4.2.2.1.51 - Boby Iyer
 * IKMAINJB-4444 : Meeting inference changes
 *
 * v4.2.2.1.52 - Carl Higashionna
 * IKMAINJB-4572 : Fix landing page flicker. Icons now fade in using 300ms alpha animation.
 *
 * v4.2.2.1.53 - Carl Higashionna
 * IKMAINJB-3287 : Calendar picker crashes during locale, font size change. Picker no longer handles those config changes.
 *
 * v4.2.2.1.54 - Ghouse Adoni
 * IKMAINJB-4525: possible Cursor leak fix in EditLocationActivity
 *
 * v4.2.2.1.54 - Ghouse Adoni
 * IKCTXTAW-530: Added Support to log only the publishers which are valid
 *
 * v4.2.2.1.55 - Carl Higashionna
 * IKMAINJB-4816 : Reverting IKMAINJB-3761 that was causing an FC at a different area in the code.
 * Fixing IKMAINJB-3761 by wrapping in a try/catch block, since the Activity has already been destroyed.
 *
 * v4.2.2.1.56 - Ivan Wong
 * IKMAINJB-4947 : Added support to allow the ringer volume picker to also affect the notification volume.
 *
 * v4.2.2.1.57 - Carl Higashionna
 * IKMAINJB-4806 : EditRuleActivity FC on locale change. Disabling config change for locale.
 *
 * v4.2.2.1.58 - Carl Higashionna
 * IKMAINJB-4528 : CheckedTextView in DriveModeSuggestionDialog not properly highlighting.
 *
 * v4.2.2.1.59 - Boby Iyer
 * IKMAINJB-5410 : Drive Mode changes to add new resources and actions
 *
 * v4.2.2.1.60 - Carl Higashionna
 * IKMAINJB-4595 : Fix FC on WifiConnectionChooserActivity.
 *
 * v4.2.2.1.61 - Boby Iyer
 * IKMAINJB-5410 : Drive Mode changes to add new resources and actions : Round 2
 *
 * v4.2.2.1.62 - Boby Iyer
 * IKJBREL1-10171 : Display sensor should return the current display state
 *
 * v4.2.2.1.63 - Carl Higashionna
 * IKMAINJB-5431 : Fixed VIP ringer to pickup contact's name change.
 *
 * v4.2.2.1.64 - Vel Pratheesh Sankar
 * IKMAINJB-4339 : Wifi still enabled in airplane mode B2GID:2273025
 *
 * v4.2.2.1.65 - Vel Pratheesh Sankar
 * IKCTXTAW-650 Rule is not active even though GPS can detect
 *
 * v4.2.2.1.66 - Boby Iyer
 * IKMAINJB-5416 : Wifi Revert issue
 *
 * v4.2.2.1.67 - Carl Higashionna
 * IKMAINJB-5390 : Fix crash in Landing Page due to clearing the ListAdapter during onPause method.
 *
 * v4.2.2.1.68 - Carl Higashionna
 * IKMAINJB-5875 : Fix SendTextMessage Action picker that was retaining the message from previous invocation.
 *
 * v4.2.2.1.69 - Ghouse Adoni
 * IKMAINJB-5979 : Drive text changes recommended from CXD
 *
 * v4.2.2.1.70 - Carl Higashionna
 * IKMAINJB-6186 : Fix ANR in HeadSetActivity that was caused by Flip keyboardOpen=true monkey test event.
 *
 * v4.2.2.1.71 - Boby Iyer
 * IKMAINJB-6158 : Voice Announce infinite loop issue
 *
 * v4.2.2.1.72 - Boby Iyer
 * IKJBREL1-10659 : Wifi Error when there is no state change
 *
 * 4.2.2.1.73 - Ivan Wong
 * IKJBREL2-647 : Fixed a potential cursor leak in the timeframe picker.
 *
 * 4.2.2.2.0 - Ghouse Adoni
 * IKMAINJB-6966 : Google Play update.
 *
 * v4.2.2.3.0 - Boby Iyer
 * IKJBREL2-2176 : Revert response for background data should send revert_response
 *
 * v4.2.2.3.1 - Vel Pratheesh Sankar
 * IKJBOMAP-2377 : Battery discharge
 *
 * v4.2.2.3.2 - Ivan Wong
 * IKJBREL2-2834 : Null-guard the configured networks query.
 *
 * v4.2.2.3.3 - Ivan Wong
 * IKJBREL2-3327 : close a possible cursor leak.
 *
 * v4.2.2.3.4 - Carl Higashionna
 * IKCTXTAW-739 : Moved call to super to end of Pause methood in LandingPageActivity.
 *
 * v4.2.2.3.5 - Carl Higashionna
 * IKJBREL2-4174 : Fixed crash issues reported in the marketplace.
 *
 * v4.2.2.3.6 - Ivan Wong
 * IKJBREL2-4174 : Fixed the IllegalArgumentException (issue 6) in the bug report.
 *
 * v4.2.2.3.7 - Craig Detter
 *  IKCTXTAW-741 : Google Play Store Crash fix.
 *  This should never happen in production unless the phone is rooted. This crash gives hackers a portal
 *  (comments collection) to blog bash on SmartACTIONS, ugh!
 *
 * v4.2.2.3.8 - Vel Pratheesh Sankar
 * IKCTXTAW-741 : Google Play Store Crash fix.
 *
 * v4.2.2.4.0 - Vel Pratheesh Sankar
 * IKCTXTAW-741 : Google Play Store Crash fix. Enable INFO level logs in production.
 *                + play store version update
 *
 * v4.2.2.4.1 - Sai Kiran
 * IKMAINJB-5514: Help page back button handling.
 *
 * v4.2.2.4.2 - Carl Higashionna
 * IKMAINJB-9042 : Fixed NPE in EditRuleActivity.refreshUserInstructions.
 *
 * v4.2.2.4.3 - Boby Iyer
 * IKCTXTAW-744 : Upgrade issues
 * 
 * v4.2.2.5.0 - Ghouse Adoni
 * IKCTXTAW-744 : Google Play store update
 * 
 * v4.2.2.5.1 - Kurinji
 * IKJBREL25-83 : Fix cursor leak
 *
 * v4.2.2.5.2 - Kurinji
 * IKMAIN-49344 : Added missing resource for drawable-mdpi
 *
 * v4.2.2.5.3 - Kurinji
 * IKMAINJB-10122 : Fix NumberFormatException
 *
 */
public class ChangeHistory {

}
