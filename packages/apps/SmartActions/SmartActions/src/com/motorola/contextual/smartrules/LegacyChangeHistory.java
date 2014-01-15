package com.motorola.contextual.smartrules;

/** Smart Rules Change History
 * 
 *  v1.1.0 - Suren
 * 	 - Initial Creation.
 * 	 - Added layouts for Landing Page and Check Settings screen.
 *   - Created reciever and service to listen to the broadcast intent by VSM and fire the actions.
 *
 *  v1.1.1 - Suren
 *   - Change to the Service and Receiver to read from Mode Manager database and fire the actions.
 *   - Change the Landing Page activity to query the Mode Manager database and use the cursor to
 *  	populate the list.
 *
 *  v1.1.2 - Saral
 * 	 - Modified the receiver and service to new database changes.
 *
 *  v1.1.3 - Suren
 *   - Modified the service and receiver to be updated to the new database changes.
 *   - Modified the landing page activity to display the right modes that are active.
 *   - Display a notification and retain it forever.
 *
 *  v.1.1.4 -Suren
 *   - In the broadcast receiver if the mode name in the received intent is the same as the
 *		current mode we are already in then ignore this request but just update the notification display.
 *		Similar logic for the leaving case also. Ignore the leaving request if the mode name in the
 *		request is not the same as the current active mode.
 *
 *  v1.1.5 - Suren
 *	 - Changes made to read the mode name from the rule table and not the condition table. Also the sensor name
 *		is now read from rule table Tags field and not sensor name field in condition table.
 *	 - Moved the notification for arrival at a mode into the service.
 *   - When storing the row tags for later use changed the tags field to key field and store it from the key column
 *  	of rule table.
 *   - Added functionality to write to the debug provider.
 *   - Added functionality to handle long press on the LandingPageActivity and display Debug option.
 *   - Integrated Craig's changes.
 *
 *  v1.1.6 - Craig
 *   - Additonal changes for debug logic.
 *
 *  v1.1.7 - Suren
 * 	 - Added try catch block around the debug provider write.
 * 	 - Unclosed cursors issue fixed.
 *   - Removed the constants used for debug viewer and replaced them with DebugTable.java columns.
 *   - Added code to write to debug viewer when every action is fired.
 *   - Added the update functionality to the provider.
 *   - Updated the rules table with the ACTIVE flag, the LAST_ACTIVE_DATE_TIME and LAST_INACTIVE_DATE_TIME.
 *   - Updated the action table with the LAST_FIRED_DATE_TIME
 *
 *  v1.1.8 - Suren
 * 	 - Modified the receiver to check for status from the rules table ACTIVE column.
 *   - Add additional data to the broadcast intent when firing actions.
 *   - Added if(LOG_XXXX) for all Log.x statements.
 *
 *  v1.1.9 - Suren
 * 	 - Changed the logic of handling of onClick and onLongClick of any list row.
 * 	 - Selecting the toggle button will enable or disable the mode. Selecting the text area
 * 		will launch rules composer. Long click of text area will show the pop up menu.
 *
 *  v1.1.10 - Suren
 * 	 - Moved the logic to check for mode arrive, leave or ignore handling to the service from the receiver.
 * 	 - Changed code to launch the service and not the broadcast receiver when user clicks the toggle button
 * 		in the landing page.
 *
 *  v1.1.11 - Suren
 * 	 - Fixed the cursor leak issues by adding the finally block and closing the cursor in there.
 * 	 - Started logic to add actions to the bucket table.
 *
 *  v1.1.12 - Suren
 * 	 - Changes to use the new debug provider (V1.6) content URI when writing the debug records.
 *
 *  v1.1.13 - Suren
 * 	 - Added functionality to select the toggle button on the landing page to opt in or opt out of
 * 		context aware.
 * 	 - Enabled the display of the Add Rule and Check Settings Button and pressing the Check Settings
 * 		button will display a dummy check settings screen.
 * 	 - List layouts modified for Landing and Check Settings Page.
 * 	 - Added code to write the correct value into the database for the Active and Inactive flag for a
 * 		rule.
 *
 *  v1.1.14 - Suren
 * 	 - Changed the URI in the debug viewer to the new debug app content URI (V1.6)
 * 	 - Added functionality to display the legal dialog when the application is started for the very
 * 		first time.
 * 	 - Added the Add rule dialog (clicking of add rules from the landing page).
 * 	 - Added functionality when user selects the copy from existing rule from the add rule dialog.
 *
 *  v1.1.15 - Suren
 * 	 - Added the About dialog functionality.
 * 	 - Moved Legal Screen dialog into the about package.
 * 	 - Implemented the logic to handle the result when returning from Rules Composer when invoked for
 * 		copy existing rule. Delete if user selected cancel or display confirmation dialog when user
 * 		selects the back key. If user selects ok then delete the rule else just exit.
 * 	 - Cleaned up unnecessary code from SmartRulesService.java.
 * 	 - Added the Suggested Rules list screen.
 *   - Added logic to display screens when the user has not opted in and has no rules, user has opted in
 *  	but no rules and user had opted in but has some rules.
 *
 *  v1.1.16 - Suren
 *   - Added Settings Menu option and also to display the Smart Rules Settings Activity when selected.
 *   - Added Balloon for the startup screen when user has not opted in and has no rules.
 *   - Fixed bug in copying the actions and conditions when copying an existing rule into a new rule.
 *   - Added dialogs to be displayed when activating a rule based on if it is a manual rule or an automatic rule.
 *   - Added clickability to Viewing Suggestions button on the screen displayed to the user when opted in and has
 *   	no rules but suggestions are available.
 *
 *  v1.1.17 - Suren
 *   - Added logic to delete entries from Condition Sensor Table when deleting a rule.
 *   - Fixed bug in Saral's code when deleting from Condition Builder.
 *   - Added code to store the user selection when manually activating an automatic rule.
 *
 *  v1.1.18 - Suren
 *   - Fixed cursor close bug issue seen due to incorrect coding.
 *
 *  v1.1.19 - Suren
 *   - Fixed more cursor close related issues.
 *
 *  v1.1.20 - Suren
 *   - Added code to copy the Condition Sensor table entries also when user selects to copy an existing rule.
 *   - Added code to store the manual override when user selects to manually turn off a rule when activating it.
 *   - Added launcher intent into the manifest for Suggestions List Activity.
 *
 *  v1.1.21 - Saral
 *   -  Canned rules support.
 *
 *  v1.1.22 - Saral
 *   -  Condition Builder updated to support new VSM changes.
 *
 *  v1.1.23 - Suren
 *   - Added new media available from CxD.
 *   - Removed the legal screen dialog and replaced it with the new about dialog.
 *   - Removed the on/off smart rules toggle button.
 *   - Replaced the rule icon toggle button with an icon.
 *   - Move add rule button to a + sign in the header for smart rules.
 *   - Support added for onClick in the Suggested Rules List.
 *   - Differentiate between manual and automatic rules.
 *   - For Automatic rules show dialog when trying to manually activate a rule.
 *
 *  v1.1.24 - Ashok
 *   - Changes to launch time frames from "Smart Rules" Settings
 *
 *  v1.1.25 - Suren
 *   - Added version 1 of homescreen widget.
 *   - Changes made to display the background color for the rule icon on the landing page.
 *   - Changes made to hide suggestions when the user opts not to display suggestions.
 *   - Changes made to store the enable and disable of the context aware preference.
 *
 *  v1.1.26 - Suren
 *   - Changed the display of the last used or currently active time for a rule.
 *   - Changed the whereClause for the suggested rules.
 *   - Added the icon to the header of the landing page.
 *   - Changed the + icon to the timer icon for the layout where there are no rules.
 *
 *  v1.1.27 - Saral
 *   - Added Rules Exporter
 *
 *  v1.1.28 - Suren
 *   - Modified the Suggested Rules Activity to take two intent extras to be used for both Suggested
 *   	Rules and Preset Rules.
 *   - Added the CA opt-in and CA opted-in dialogs that will be displayed to the user.
 *   - Cleaned up the delete rule code by consolidating it into one function in the DbUtils file.
 *
 *  v1.1.29 - Saral
 *   - Transition from RuleView to Rule table usage in Rules Exporter & Condition Builder
 *
 *  v1.1.30 - Suren
 *   - Consolidated drawables with Paramita (Puzzle Builder)
 *   - Added new layout for widget and cleaned up the widget display.
 *   - Changed the layout for Suggested Rules to be generic.
 *   - Added logic to display the last used or active since like in Phone Book.
 *
 *  v1.1.31 - Vignesh on behalf of Ashok
 *   - String interface for rulesimporter
 *
 *  v1.1.32 - Suren
 *   - Added code to read and display the rule icon form the database.
 *   - Changed Home Screen Widget to launch Landing Page on any of the id's.
 *
 *  v1.1.33 - Saral
 *   - Rues Importer Change to take icons at Rule & Action Level
 *
 *  v1.1.34 - Suren
 *   - Finish the display rules activity if there are no rules to display once the activity resumes
 *   	from Puzzle Builder.
 *   - Modified logic to hide and show the suggestions bar for onResume() case.
 *
 *  v1.1.35 - Saral
 *   - Enabling Rues Exporter
 *
 *  v1.1.36 - Suren
 *   - Commented out code to display the opted in dialog.
 *   - Changed the where clause for the copy exisiting rule activity to be the same as landing page activity.
 *
 *  v1.1.37 - Suren
 *   - Added logic to ignore a manual rule process if coming from VSM.
 *   - Added code to display rule icon in the Display Rules Activity.
 *   - Removed the rule icon background from the list row layout.
 *
 *  v1.1.38 - Saral
 *   - Condition Builder has been enhanced to support the new VSM interface changes.
 *   - New option for precanned rules added in Rules Exporter.
 *
 *  v1.1.39 - Saral
 *   - Cursor close issue fixed in Condition Builder.
 *
 *  v1.1.40 - Saral
 *   - Condition Builder Code Clean up Phase I.
 *
 *  v1.1.41 - Suren
 *   - Used the + icon created by Tom.
 *   - Fixed the issue of the bottom bar being hidden when the very first rule is created and returned from
 *   	puzzle builder.
 *
 *  v1.1.42 - Saral
 *   - Condition Builder Code Clean up Phase II.
 *
 *  v1.1.43 - Suren
 *   - Cleaned up the whereClause used in Landing Page and Copy From Existing Rule.
 *   - Package name changed from com.motorola.contextual.modemanager to com.motorola.contextual.smartrules
 *
 *  v1.1.44 - Suren
 *   - Changed the _id in Rule, Action and Condition table to a long from int.
 *
 *  v1.1.45 - Suren
 *   - More code cleanup.
 *   - New VSM JAR files added to vsm_jar directory.
 *
 *  v1.1.46 - Suren
 *   - Merged Puzzle Builder code into Smart Rules and modified the app related code that launches puzzle
 *   	builder.
 *   - Cleaned up unused icons.
 *
 *  v1.1.47 - Saral
 *   - Precanned rules to be read during powerup.
 *
 *  v1.1.48 - Saral
 *   - Rules Importer, Rules Exporter & Canned Rules Importer enhanced to support
 *     Active & Enabled changes in Mode Manager DB
 *
 *  v1.1.49 - Suren
 *   - Modified icons with new media from Daniel.
 *   - Removed and balloon and made it into an in-line layout when no rules are present.
 *   - Modified the activation dialog for Automatic rules.
 *   - Modified the logic to display the options that are shown to the user when the + sign is
 *   	selected on the landing page.
 *   - Added logic to differentiate Suggested and Preset rules in the display rules list activity
 *   	and exit for preset rules when an onActivityResult is called.
 *   - Modified the copy rule activity to show up as the latest layout.
 *   - Removed the about dialog from being displayed during the first time app launch.
 *   - Cleaned up strings.xml file to remove unused strings.
 *
 *  v1.1.50 - Saral
 *   - Timing issue fixed in launching Inference Manager
 *
 *  v1.1.51 - Suren
 *   - Added additional extra for puzzle builder launch for copy rule.
 *   - Replaced hard coded strings in puzzle builder with constants.
 *   - Landing page layout modified to use a scroll view when no rules are present.
 *   - Added new button More Info to the bottom bar and hide/unhide More Info and Current Settings
 *   	button based on if there are any rules or not.
 *   - Removed the text clickability for more info.
 *
 *  v1.1.52 - Suren
 *   - Added functionality to show a dialog when user selects to delete a rule.
 *   - Added new VSM JAR files.
 *
 *  v1.1.53 - Saral
 *   - Klocworks & findbugs fix for conditionbuilder module
 *
 *  v1.1.54 - Suren
 *   - Conflict Resolution added for when a rule is invoked via VSM and also by the user when the
 *   	rule icon is selected.
 *   - Remove Rule and related text changed to Delete Rule and other strings.
 *   - Removed the popup dialog when rule icon is clicked and new logic implemented with conflict
 *   	resolution.
 *   - Listener added to listen to Quick Actions response and store the default value sent as required.
 *
 *  v1.1.55 - Saral
 *   - Precanned xml updated for factory rules (SOURCE = 4) with KEY & Sensor name as
 *    RuleName:<Javadatetime>
 *   - MODAL value for Actions are updated in the precanned xml for all rules.
 *   - Rules Importer, Exporter & Rules Importer Precanned modules are updated to
 *     accomodate "MODAL" field
 *
 *  v1.1.56 - Saral
 *   - MODAL value for Conditions are updated in the precanned xml for all rules.
 *
 *  v1.1.57 - Paramita
 *   - Puzzle Builder changes for actions and conditions etc...
 *
 *  v1.1.58 - Suren
 *   - Added new column Rule Type to Rule Table and deprecated the Manual Override column.
 *   - Made some changes for the Look 10 layout.
 *   - Added code and logic to display the notification when a rule gets activated.
 *   - Added code logic to restore the default value.
 *
 *  v1.1.59 - Paramita
 *   - Added code to populate modality in Action Table
 *   - Removed populate modality code from LandingPageActivity.java
 *   - Changed the metadata type for modality in ConditionTable.java
 *
 *  v1.1.60 - Suren
 *   - Added logic to listen to database updates and refresh the landing page list.
 *   - Fixed error when writing the RuleType field to the database.
 *   - Added Source type Default for the Default rule and modified the initial record creation
 *  	to set this as the value.
 *
 *  v1.1.61 - Paramita
 *   - Added null check for the stateless/stateful type and default to stateful if not present in manifest
 *   - Populating target state field in the Actions Table
 *
 *  v1.1.62 - Saral
 *   - Condition Builder: Connect, Disconnect support for conditions.
 *
 *  v1.1.63 - Saral
 *   - Condition Builder ANR fixed
 *   - RULE_TYPE field support for Rules Importer, Exporter & Precanned xml
 *
 *  v1.1.64 - Paramita
 *   - Removed the call to populateModality(), till the Database open issue is sorted out
 *   - Write modal values to the Action table as and when each action is inserted.
 *   - Log cleanup
 *
 *  v1.1.65 - Suren
 *   - When delete and disable all options are selected need to handle conflict resolution. Code added to
 *    	handle this.
 *   - Removed unecessary refreshes of list. Will be refreshed only when DB is updated.
 *   - Code completed for Current Settings and Override Settings activities.
 *
 *  v1.1.66 - Paramita
 *   - Write Target state for preconditions to DB
 *   - Delete Rule from Menu
 *
 *  v1.1.67 - Suren
 *   - Write record into debug viewer db when conflictis seen for an action and it is not fired.
 *   - Modifed the current settings layouts.
 *
 *  v1.1.68 - Paramita
 *   - Write State Machine Name to ActionTable. This was earlier done in the populateModality function, which has been removed.
 *
 *  v1.1.69 - Suren
 *   - Added logic so that the user cannot delete the default rule. A toast is displayed.
 *   - If the default rule is deleted then it will be created when the first time default record is required for conflict resolution.
 *   - Changes made to have look 10 show up for the landing page and other screens as well.
 *
 *  v1.1.70 - Saral
 *   - Condition Builder: Merge conflict : Craig's Changes reverted.
 *   - Action Name support in precanned rules based on rulesbuilder change in version v1.1.68
 *
 *  v1.1.71 - Suren
 *   - Added logic to receive the intent from Quick Actions when the user changes the setting value manually via Settings app.
 *   - Added logic to store and restore the clicked row info in Landing page to handle the crash seen when delete dialog is shown.
 *
 *  v1.1.72 - Suren
 *   - Replaced the old delete rule call with the new delete rule call.
 *   - Added logic to send the notify change to DB update listeners when a rule is deleted.
 *   - Call conflict resolution of rule on deletion only if it is active.
 *   - Invoke condition builder only for auto rules when deleted.
 *
 *  v1.1.73 - Paramita
 *   - Code changes for populating the Enabled and the RuleType columns in the Rule Table in various scenarios
 *
 *  v1.1.74 - Paramita
 *   - Code changes for writing the rule icon path name to DB (to replace the rule icon resource ID)
 *
 *  v1.1.75 - Suren
 *   - Additional Look 10 changes for titlebar.
 *   - Logic fixed when displaying the landing page when rules cursor is empty.
 *
 *  v1.1.76 - Suren
 *   - Added logic to read the icon from the DB and display it.
 *   - Added additional graphics for the white background.
 *   - Updated the xml file in the assests directory so that rules importer can add the correct icon string to DB.
 *   - Added logic to display the correct icon in the notification bar.
 *
 *  v1.1.77 - Suren
 *   - Updated the XML with icons for Inferred and Suggested rules.
 *
 *  v1.1.78 - Saral
 *   - Condition Builder & Rules Importer Code Clean up.
 *   - For Look 10, removed the default android:theme="@android:style/Theme.NoTitleBar"
 *     in the RulesExporterImporterActivity
 *
 *  v1.1.79 - Suren
 *   - Fixed logic of handling the delete rule so that condition builder is called correctly.
 *
 *  v1.1.80 - Suren
 *   - Applied Look 10 changes to Settings Activity and Buttons across the project.
 *
 *  v1.1.81 - Paramita
 *   - Write ON_MODE_EXIT values to DB.
 *
 *  v1.1.82 - Suren
 *   - Added the title bar for Rules Builder and updated the title bar layout to show icon and edit text.
 *   - Cleaned up unused drawables and deleted the header layouts that were used for Rules Builder.
 *   - Updated the Current Settings activity not to show stateless actions on it.
 *
 *  v.1.1.83 - Paramita
 *   - Fine tuned the Action and Condition list icons
 *
 *  v1.1.84 - Suren
 *   - Remove Default Rule from the rules list and add a Debug Default Rule to the long press menu.
 *   - Do not update notification if we are not processing the pending intent for a rule from VSM.
 *   - Accepted code review changes by Craig.
 *   - Update the last active time in rule table for a rule only if it was active and is being disabed
 *   	via disable all option.
 *   - The notification icon to be only the app icon for all rules. The rule icon will not be displayed
 *   	anymore.
 *   - Reworded the list displayed when the user selects to add a new rule.
 *
 *  v1.1.85 - Suren
 *   - Fixed bug for stateless actions to check for ON_MODE_EXIT column to see if it is set to ON_ENTER
 *      or ON_EXIT and fire accordingly.
 *   - Commented out the code that was firing to update the homescreen widget.
 *
 *  v1.1.86 - Saral
 *   - ConditionBuilder & RulesImporter modules are incorporated with review comments
 *     from Craig
 *
 *  v1.1.87 - Suren
 *   - Set the opted in and opt in dialogs to be white background and the text to be black.
 *   - Modified the icon to show it on top of the inactive backround for copy rule, preset and
 *   	suggestion rules list.
 *   - Added grey bar to the title of copy rule activity.
 *   - Changed the title text to be medium size for Current Settings screen and also align the
 *   	rule name and rule icon to be aligned to the right of list.
 *   - Changed the style for "Your Rules" in landing page to be bold.
 *   - Changed order and wording of the add rule dialog.
 *   - Fixed issue with the layouts to show correctly when rules are there and not there along with
 *   	suggestions etc... in landing page.
 *   - Changed "Enable Context Awareness" to "Enable Context Aware" in the profile and settings scren
 *   	and also added title to the screen.
 *
 *  v1.1.88 - Paramita
 *   - Code cleanup for review
 *   - Fixed bug that got introduced with Mode exit changes (Disabling/Enabling an action block would delete it)
 *
 *  v1.1.89 - Suren
 *   - Add the blue background to the number of smart rules suggestion shown to user on landing page.
 *   - Changed the background of Your Rules and also the text color.
 *   - Bold the copy rule header shown.
 *   - Added the new Look 10 line to the landing page row.
 *   - Added new icon for the disabled state.
 *
 *  v1.1.90 - Saral
 *   -  Final review comments from Craig
 *
 *  v1.1.91 - Paramita
 *   - Code cleanup for review
 *
 *  v1.1.92 - Paramita
 *  - Added comments for review
 *
 *  v1.1.93 - Suren
 *   - Added copyright and class definition to few classes.
 *   - Disable all to call condition builder service.
 *   - Left align the suggested rules displayed in landing page.
 *
 *  v1.1.94 - Saral
 *   -  Updated the rulesimporter.rules (preset rules) to accomodate the new Quick Actions
 *      intent / uritofireaction / Activity Name / Desc changes
 *
 *  v1.1.95 - Paramita
 *   - Fixed some issues from issue list (Rename rule dialog should persist previous name and Delete confirmation dialog should show up)
 *
 *  v1.1.96 - Suren
 *   - Change the preset rule icons from black to white.
 *   - Changed the spacing between the lines when displaying preset and suggested rules.
 *   - Changed the color of the text in the title for Current Settings list.
 *   - Changed the color and size of Your Rules to the same as copy rule activity.
 *
 *  v1.1.97 - Saral
 *   - Condition Builder : Support for special characters : Inference Requirement
 *
 *  v1.1.98 - Suren
 *   - Look 10 action bar changes for layouts, added drawables, xml files and updated code as needed for using the new action bar.
 *
 *  v1.1.99 - Paramita
 *   - Changes for logging Condition information to Debug Provider
 *
 *  v1.1.100 - Saral
 *   -  Condition Builder : Support for special characters in rule name
 *
 *  v1.1.101 - Suren
 *   - When deleting or rule status change via the rule icon selection make sure to call
 *   	condition builder service always irrespective of rule type.
 *   - When a handling rule inactivation move the code to process the rule into the finally
 *   	block to handle the case where action cursor is null for the rule under process and hence
 *   	the conflict resolution will never be called and the rest of the code will not be called.
 *
 *  v1.1.102 - Paramita
 *   - Review comments from Craig
 *
 *  v1.1.103 - Paramita and Rahul
 *  - Replaced with new UI
 * 
 *  v1.1.104 - Suren
 *   - Added new drawables from Daniel and removed the old drawables.
 *   - Current Settings and Override Settings activities to show the icon for the setting and rule.
 *   - Landing Page menu and design changed as per discussion with CxD.
 *   - Removed the button around the rule icon for Suggested and Preset Rules list.
 * 	 - Split up Profile and Settings into two individual activities.
 *   - Fixed the editing case where an auto rule becomes a manual rule in puzzle builder.
 *  
 *  v1.1.105 - Suren, Rahul and Paramita
 *   - Latest Puzzle Builder layout changes from Rahul and Paramita.
 *   - Added white icons and replaced with new icons from CxD.
 *   - Added Help Activity and related files.
 *   - Minor changes to display as per CxD discussions.
 *   - Fix compile issues with package name associated with inference manager changes merged into Smart Rules.
 * 
 *  v1.1.106 - Suren & Rahul
 *   - Integrate the two lines interactive title bar layout and add functionality to display the correct text
 *   	in puzzle builder for active, ready and disabled rules along with the icon button background.
 *   - Add changes to show Error for a rule on the Landing Page.
 *   - Added latest rulesimporter.rules file to the assets folder.
 *   - Added latest help.html file to the assets folder.
 *   - In Puzzle Builder fixed the scroll layout so that the last element is not covered by the action bar.
 *  
 *  v1.1.107 - Suren & Rahul
 *   - Added new icons from Daniel (CxD)
 *   - Rules Builder layout changes as per CxD.
 *   - Fix a log issue in Provider.
 *   - New rulesimporter.rules file.
 *   
 *  v1.1.108 - Paramita
 *   - Added dialogs for Discard rule, Back Key press, Manual rule confirmation
 *   - End of rule status displayed on action block 
 *   - Adding black background to Action and Trigger list and keeping the icons white
 *   - List separator for Action and Trigger list and the Rule name displayed on the same list.
 *   - Fixed Issue # 114 from user trial issues spreadsheet.
 *   - Hide Action Bar in the Summary screen and show the Action Bar in the case of Edit, Preset and Suggested rules.
 *   - Added extra icons in Rule Icon Picker.
 *   
 *  v1.1.109 - Suren
 *   - Added grey background to all list layouts.
 *   - Added functionality for the Check Status as per CxD redesign.
 *   - Fix the hide/unhide of error layout in list row code of landing page.
 *  
 *  v1.1.110 - Suren
 *   - Added new drawables from Daniel.
 *   - Changed display of Error/Suggestion etc... on landing page.
 *   - Added long press functionality to title bar in Puzzle Builder and to show save, cancel buttons.
 *   - Added updated functionality for settings activity.
 *   - Added functionality check for show rule notifications only if the preference is selected.
 * 
 *  v1.1.111 - Suren
 *   - Removed Inference Manager related code out of Smart Rules.
 *   - Added the column Suggested Type to Rule, Action and Condition table.
 *   - Added the columns Suggested Reason and Failure Message to Condition and Action Tables.
 *   - Added the columns Condition Met and Last Fail Time to Condition Table.
 *   - Added the column Conflict Winner to the Action Table.
 *   - Cleaned up drawables that are not used.
 *   - Fixed the grey bar issue seen in puzzle builder right below the title bar.
 *   - Added inference rules to the rulesimporter.rules file.
 * 
 *  v1.1.112 - Suren
 *   - Added column Lifecycle to Rule Table.
 *   - Updated the tuples from a string concat to a StringBuilder changes.
 *   - Updated the latest rulesimporter.rules file.
 *   - Reverted fix for issue # 114 from the user trial issues excel sheet.
 *        
 *        
 *  v1.1.113 - Paramita
 *   - Added dialog for discard sample or suggested rule
 *   - Made default rule name as the first trigger name, instead of New Rule
 *   - Retrieves block name from State Machine name for Actions. (for sample rules)
 *  
 *  v1.1.114 - Suren, Rahul and Paramita
 *   - Changes made to handle the icon press when viewing a rule in puzzle builder. The rule icon
 *   	button press should behave the same way as in landing page.
 *   - Changes to show starting up and shutting down for a rule in both Landing Page and Puzzle
 *   	Builder if the rule becomes active via VSM Pending Intent.
 *   - Changes to display error message in Check Status Page if no rule is controlling the actions.
 *   - Added latest rulesimporter.rules from Venki and Vignesh.
 *   - Changed the height of the interactive title bar so that the rule icon shows up nicely in the 
 *   	title bar.
 *   - Changed the long press to a Contextual menu and not an alert dialog in Landing Page activity.
 *   - Redesigned the flow for picking an action or condition.
 *   - Hardcoded the conflicting trigger action pairs.
 *   - Added Silent column to the RuleTable to indicate if the notification needs to be shown for the
 *   	rule when it is activated.
 *   - Added logic to write the conflict winner column value into action table.
 *   - Layout changes for rules builder.
 *
 *  v1.1.115 - Suren
 *   - Fixed the compile issues due to adding silent column to the rule table. Needed to update the
 *   	RulesImporter.java file that was using the RuleTuple code.
 *   - Added the missing rules from the rulesimporter.rules file.
 *   - Changed Silent column from long to an int in Rule Tuple and updated other places accordingly.
 *   - Added the default value when the Silent column is added to the Rule Table during DB upgrade.
 * 
 *  v1.1.116 - Suren
 *   - Added animation (part 1) to the rule icon on the landing page.
 *   - Updated with new icons from CxD.
 *   - Refactored code in Rule that is being called when rule icon is selected in landing page.
 *   - Fixed issue when deleting a manual rule.
 *   - Added code to populate the condition met field in the Condition Table.
 *   - Redesigned the Check Status code to use the Conflict Winner flag from the Action Table.
 *   - Comments from code review of v1.1.115 incorporated.
 *   - Logic added not to launch multiple instances of the Landing Page Activity when selecting
 *   	from notification bar.
 *  
 *  v1.1.117 - Suren
 *   - Layouts modified to show the gray list background. Removed the LinearLayout wrapper around
 *   	the list to make this work.
 *   - Layouts modified to show feedback when user selects a list item.
 *   - Added permission for location sensor.
 *   - Removed the icon for the Rules Builder app from the app tray.
 *   
 *  v1.1.118 - Paramita
 *   - Updated UI as per CXD v4.5. The main changes are:
 *     Different interpretation of block status lights
 *     Title bar changes for rename rule and edit icon
 *   - Fixed the following issues
 *     "Starting up" showing up when a rule in the background is starting up
 *     Null pointer checks so that when some other apps crash, puzzle builder should not crash.
 *  
 *  v1.1.119 - Suren
 *   - Cleaned up unused layouts.
 *   - EditText to start with an uppercase letter for Rule Name.
 *   - Added animation to the rule icon when rules is activated/deactivated in rules builder screen.
 *   - Added opacity to the disabled rules and also the check status icon when no rules are active
 *   	on the landing page.
 *   - Updated rulesimporter.rules with new rules from Venki.
 *
 *  v1.1.120 Rahul
 *   - Updated Blocks UI gestures, along with other CXD requirements
 *
 *  v1.1.121 - Rohit
 *   -  IKINTNETAPP-178 - Added Suggestions UI related activities, receiver, layouts, strings, etc.
 *   
 *  v1.1.122 - Suren
 *   - Redesigned the broadcast recievers to start an IntentService to handle the intents and not create
 *   	a thread within the reciever.
 *   - Added shadow to title bar layout.
 *   - Removed showing of text when an automatic rule is activated/inactivated via VSM.
 *
 *  v1.1.123 - Rohit
 *   - Notification updates, removal once suggestions are read in the app
 *   - rulesimporter.rules xml fields updated
 *   - try-catch added in some methods
 *   - Notification enabled thru preference settings
 * 
 *  v1.1.124 - Suren
 *   - Check the intent actions in broadcast receivers before starting the services.
 *   - Added more checks for null when handing the intent from VSM in Landing Page.
 *   - Renamed Rule to RulePersistence class.
 *   - Added the action table related DB handlers to ActionPersistence class.
 *   - Check for null input parameters before calling the handlers.
 *   
 *  v1.1.125 - Paramita
 *   - UI changes as per spec
 *   - Fixed issue in Puzzle builder where in some cases duplicate blocks were being added to the screen.
 *   - Disable Save button so that a blank rule cannot be saved, or a rule with only triggers cannot be saved.
 *   - Blocks will not disappear when keyboard opened and closed.
 *  
 *  v1.1.126 - Suren
 *   - Moved code related to the DB tables into their correspondong Persistence Classes.
 *   - Replaced the ongoing and suggestion notification icons.
 *   - Cleaned up unused code and files.
 *
 *  v1.1.127 - Rohit
 *   - Support for adding New action in Exiting Suggestion
 *   - Initial welcome screen added
 *   - New suggestion count added on Landing page
 *   - Common code moved to Suggestion class
 *   - ongoing design improvements
 *   
 *  v1.1.128 - Suren
 *   - Added updated media from CxD.
 *   - Fixed random crash in landing page - register broadcast reciever for VSM intent in onResume()
 *   	and unregister in onPause(). Fixed similar code in Rules Builder - only register if we are in
 *   	summary mode and unregister once we get into edit mode. Similar case is for content observer
 *   	that is present for DB updates.
 *  
 *  v1.1.129 - Suren
 *   - Removed the opacity of the check status icon from landing page when no rules are active
 *   - Added logic to handle the Failure execution status from Quick Actions for any action and
 *   	update the action table accordingly.
 *   
 *   
 *  v1.1.130 - Rahul
 *   - Added new Blocks RulesBuilder UI for gesture based support
 *   - Fixed comments from Earlier review
 *   - Fixed fingbugs issues
 *       
 *  v1.1.131 - Paramita
 *   - Handling Editing a Rule when Active
 *   - Fixing unnecessary popping up of Discard changes dialog box
 *   - Some findbugs fixes
 *   - Code for supporting Suggestion and Error cases
 *   
 *  v1.1.132 - Paramita
 *   - Show up Suggestion status line only when the action/condition is marked as disabled.
 *
 *  v1.1.133 - Rohit
 *   - Added support for showing confirmation screen to configure conditions or actions
 *   - Fixed KW Errors
 *   - Minor design updates
 *   
 *  v1.1.134 - Paramita
 *   - Fixed icon color issue seen with check status
 *   - IKMAIN -15000 - CXD changed the design spec for conflicts. Now the blocks should not be disconnected
 *   - Changed logic for Edit mode in the case of sample rules.
 *   - Updated rulesimporter.rules
 *   - Changes in SuggestionDetailsActivity.java
 *
 *  v1.1.135 - Craig 4/27/11
 *   - Fixed findBugs issues in these:
 *       DbUtil, SQLiteContentProvider, ActionTable, ConditionBuilderTable, ConditionSensorTable,
 *          ConditionTable, RuleTable, TableBase
 *  
 *  v1.1.136 - Suren
 *   - Fixed findbugs issues related to setViewValue casting without checking and couple other redundant
 *   	null checks.
 *   - Updated rules importer file with the changes for inference rules.
 *
 *  v1.1.137 - Rohit
 *   - Logic to update Condition Sensor table and Rule table to add sensors
 *   - Suggestions code moved to new suggestions package
 *   - Init screen logic updated
 *   - Long press -> delete added to Inbox screen
 *   
 *  v1.1.138 - Paramita
 *   - Fixed Findbugs issues - made the ActionList and ConditionList a separate class instead of static
 *
 *  v1.1.139 - Rohit
 *   - Added View option to Long press dialog
 *   - Minor code refractoring
 * 
 *  v1.1.140 - Suren
 *   - Landing Page for no rules implemented as per latest CxD redesign.
 *   - Fixed the list background for actions and triggers list.
 *   
 *  v1.1.141 - Paramita
 *   - Incorporated review comments
 *   - Fixed issue of 2 rules showing up at times
 *   - Edit while Active usecase - made code changes to disable and enable a rule
 *
 *  v1.1.142 - Rohit
 *   - Fixed Suggestions Klocworks & Findbugs issues
 *
 *  v1.1.143 - Rohit
 *   - Fixed one time action suggestion breakage
 *   - Fixed "Needs to Configure" issue if suggestion is edited and then saved
 *   - Added space below Why/When block
 *   - Changed Ok to OK, per Adam Miller's mail
 *   - Changed design to always go to Inbox from Landing page
 *   - Minor code refactoring
 *
 *  v1.1.144 - Rohit
 *   - Fixed crash reported by Suren
 *   - Landing page updated to always show Suggestions bar
 *   - Landing page context menu changed - Suggestions removed, about added
 *   - Code refactored - Database related moved to SuggestionsPersistance class
 *  
 *  v1.1.145 - Suren
 *   - Updated new media from CxD.
 *   - Merged Help and About Web View activities into one.
 *   - Fixed other minor issues from CxD.
 *   - IKINTNETAPP-198: Fixed few Klocwork Issues
 *   - IKMAIN-16204: Fix to solve the disable/enable rule issue and call condition builder
 *   	after the rule is enabled or disabled.
 *   - IKINTNETAPP-222: Following changes
 *   	- Change Profile to My Profile
 *   	- For Copy Rule and Sample Rule activities not to exit when user cancels or
 *   		selects back key
 *   
 *  v1.1.146 - Suren
 *   - IKINTNETAPP-225: Added xhdpi and mdpi media. Cleaned up common media and 
 *   	moved them to drawables folder. 
 *
 *  v1.1.147 - Rohit
 *   - Code refactoring
 *   - Suggestions related UI changes on Landing Page
 *   - One time Action Configuration added
 *   - Inbox refresh issue fixed
 *   - VSensor tag string moved to Constants
 *   - Added support to delete New Action Suggestion
 *   
 *  v1.1.148 - Paramita
 *   - IKINTNETAPP-220 - Make changes to rule key - com.motorola.contextual.rulename.dateandtime
 *   - IKMAIN-16538 - Some of the changes reported in this CR:
 *      Progress dialog title
 *      Add rule for Create from scratch and Copy 
 *      Getting rid of Blinking cursor in Edit box when Done pressed
 *      Append title bar in Action and Condition list with - Rule Editor
 *   - Some Klocwork fixes
 *   
 *  v1.1.149 - Suren
 *   - IKINTNETAPP-198: Fixed few Klocwork Issues
 *   - IKINTNETAPP-222: Added animation states as per CxD
 *   	- Fix the issue when delete the last rule and have to show no rules layout and this deleted rule
 *   		still shows up.
 *   - IKINTNETAPP-231: Refresh the settings screen when a rule becomes active/inactive in the background.
 *   
 *  v1.1.150 Suren 
 *   - IKINTNETAPP-198: Fixed more Klocwork Issues
 *   - IKINTNETAPP-221: Make changes to rule key - com.motorola.contextual.rulename.dateandtime
 *
 *  v1.1.151 - Rohit
 *   - For Add action Suggestion, if action added from PB, remove suggestion and its notification
 *   - For OneTime Suggestion - Remove 'Customize' option
 *   - Suggestion Inbox is refreshed when RuleTable is updated
 *   
 *  v1.1.152 - Craig
 *   - General KW clean-up
 *  
 *  v1.1.153 - Suren
 *   - Add additional check to the whereClause to not show up active up invisible rules in the notification
 *   	curtain for ongoing rules.
 *  
 *  v1.1.154 - Suren
 *   - Fixed the breakage in the whereClause from v1.1.153 
 *    
 *  v.1.1.155 - Paramita
 *  - Fixed the 3rd status line trunctation issue
 *  - Turned on the production mode flag
 *  - Increased time between disable and enable for Edit while active case.
 *  
 *  v1.1.156 - Rohit
 *  - IKINTNETAPP-239 - Invisible Triggers should not be shown in Suggestion pop-up
 *  - IKNNTNETAPP-240 - Suggestion should be on the rule in LandingPage only when the 
 *    rule has a new action
 *  - In case of misformed XML, we still show the dialog
 *  - Notification state is maintained between power cycle
 * 
 *  v1.1.157 - Suren
 *   - IKINTNETAPP-244: Ongoing Smart Rules notification to be re-displayed on power up.
 *   - IKINTNETAPP-231: In CheckSettingsActivity register for ACTION Table notify change 
 *   	instead of Rule Table.
 *   - IKINTNETAPP-257: Disable plus button click after user selection and reneable when
 *   	the add rule dialog is dismissed.
 *   - IKINTNETAPP-245: Add a progress dialog when user selects to delete a rule.
 *   - IKINTNETAPP-253: Retain Alert Dialog on the screen when orientation changes.
 *   - IKMAIN-15879: Condition Builder changes to have only one thread executor and not multiple.
 *   - IKINTNETAPP-198: Fixed more Klocwork Issues
 *   
 *  v1.1.158 - Suren
 *   - IKMAIN-15879: Fix the issue introduced via fix for IKINTNETAPP-245. Need to move the if condition to only
 *   	remove message from the queue but not for posting the handler message. 
 *   
 *  v1.1.159 - Rahul
 *   - IKMAIN-16547 IKMAIN-16055 IKINTNETAPP-241 fixed UI issues in RulesBuilder
 *   
 *  v.1.1.160 - Paramita
 *   - IKINTNETAPP-289 Display Error in block when Location functionality not available
 *   
 *  v1.1.161 - Paramita
 *   - IKINTNETAPP-275 - "Add Rule" disabled for ever if the user cancel the Manual rule confirmation.
 *   
 *  v1.1.162 - Craig
 *   - IKINTNETAPP-195 - Clean-up logic error in unclosed cursor in ActionPersistence
 *                     - add some logging to Provider.java
 *                     - cleaned-up some garbage characters in Util.java.
 *                     - fixed incorrect log statement in SQLite Manager
 *                     
 *  v1.1.163 - Suren
 *   - IKINTNETAPP-264 - clear/show ongoing notifications if user unchecks/checks the settings preference.
 *   - IKINTNETAPP-268 - Fix the force close seen in landing page when delete rule progress dialog is shown
 *   						and user slides out the keyboard.
 *   - IKINTNETAPP-273 - Fix the force close seen when launch application progress dialog is shown and 
 *   						orientation is changed by the user.   
 *   
 *  v1.1.164 - Suren
 *   - IKINTNETAPP-262 - fix the alignment of rule buttons in summary and rule mode (layout changes). 
 *   
 *  v1.1.165 - Suren
 *   - IKINTNETAPP-272 - Error to show on landing page for failed actions and remove it for conditions.
 *   - Refactor the landing page to move the expensive database operations to an Async Task and not run
 *		in the main thread. 
 *
 *  v1.1.167 - Suren
 *   - IKINTNETAPP-284 - Fix icon coloring issues on Landing Page and Check Status activities.
 *   
 *  v1.1.168 -Paramita
 *   - IKINTNETAPP-283 - Renaming rule should bring up soft keyboard
 *    
 *  v1.1.169 - Paramita
 *  - Pressing Rename from Menu while in Edit mode should also bring up soft keyboard. 
 *  
 *  v1.1.170 - Paramita
 *  - IKINTNETAPP-255 - For any rule (manual or automatic), if only the icon has been changed, then the rule will not be disabled,
 *    but will continue to be in whatever state it was already in. Save after changing the icon only will not bring up
 *    the confirmation dialog for manual rule.
 *    
 *  - IKINTNETAPP-282 - Edit Icon should show Save/Discard dialog.
 *  
 *  v1.1.171 - Rohit
 *    IKINTNETAPP-295 - Suggestions green box is shown only when SuggState != Accepted in action block
 *    IKINTNETAPP-298 - Actions are enabled by Suggestion UI only for partial rules (new actions)
 *    
 *  v1.1.172 - Paramita
 *    Edit Rule icon color fix
 *    IKINTNETAPP-258 - Discard changes dialog should show 'Add Rule' instead of 'Save'
 *
 *  v1.1.173 - Rahul
 *    Minor UI changes to RulesBuilder layout+api's to support compact views inside layout
 *
 *  v1.1.174 - Vignesh karthik
 *    Changes to .next content in rulesimporter.rules to show contact names in Puzzle builder for missed call/sms suggestion
 *    
 *  v1.1.175 - Suren
 *   - IKINTNETAPP-290: Changes to list sizes as per CxD Recommendations.
 *   - IKMAIN-18149: Add missing period at the end of the error string.
 *   - IKINTNETAPP-318: Updates as per CxD recommendations.
 *   		- Add updated drawables
 *   		- Change the title bar shadow values so that the shadow is up and not down.  
 *   		- Change color for the icon shown on Check Status layout.
 *   		- Show different error messages for empty suggestions and sample rules list.
 *   		- Change the icons for Max Battery Saver and Night Time Batter Saver Sample Rules.
 *   
 *  v1.1.176 - Rohit
 *   -  IKINTNETAPP-321: Only New Suggested actions should be Enabled/Connected by Suggestions UI
 *   
 *  v1.1.177 - Paramita
 *   - IKINTNETAPP-270 - Don't show up any discard dialog for invalid rule till CXD comes up with a new dialog.
 *   - Klocwork issues
 *   - Code cleanup
 *  
 *  v1.1.178 - Suren
 *   - IKINTNETAPP-314: Changed wording in add new rule dialog.
 *   - Added StrictMode checker function in the Utils and called it from LandingPageActivity
 *   - Fixed Klocwork NPE in DisplayRulesActivity
 *   
 *  v1.1.179 - Suren
 *   - IKINTNETAPP-324: Fix breakage in Froyo due to addtion of StrictMode code.
 *   - IKINTNETAPP-325: Enable add rule button if user selects back key on add rule dialog. 
 *   
 *  v1.1.180 - Suren
 *   - IKINTNETAPP-324: Modify the strict() handler in Util.java to directly refer to 
 *   					android.os.StrictMode and remove the import.
 *   
 *  v1.1.181 - Paramita
 *   - IKINTNETAPP-248 - Fixed breakage in earlier push. Missed out the AND in the derived sensor string.
 *   
 *  v1.1.182 - Suren
 *   - IKINTNETAPP-326: Fix breakages in conflicts and default records reset
 *   		- Fetch only Active actions recrods when requesting the coflict cursor for an action.
 *   		- Change the extra to be read from when the settings change intent comes in from Quick Actions.
 *   		- Set the Uri to fire to null in the default record when settings change happens and also reset
 *   			the conflict winner flag. 
 *   
 *  v.1.1.183 - Paramita
 *   - IKINTNETAPP-248 : Remove non-alphanumeric characters in rule key (Except '.' and '%')
 *   
 *  v.1.1.184 - Vignesh
 *   - Rulesimporter importing RTI - changes in logic to account change ENABLED tag
 *  
 *  v1.1.185 - Suren
 *   - IKMAIN-19291: Handle orientation changes for Suggestions dialog activity.
 *   - IKINTNETAPP-334: Handle user selection on search button on add rule dialog.
 *   - IKINTNETAPP-329: Exit GraphicsActivity if the rule to view cannot be read from DB.
 *   - IKINTNETAPP-332: Implement help related changes to point to the CxD content.
 *   - Modify the debug viewer write to an async task and not write in the main thread and merge the two
 *   	debug viewer write functions into one.
 *   - Clean up few dead store findbugs issues and add the exclude file for the others.
 *   
 *  v1.1.186 - Suren and Paramita
 *   - IKMAIN-19785: Use context menu instead of an alert dialog in suggestions inbox
 *   - IKMAIN-19603: Layout changes to show the row and text for rule name correctly.
 *   - IKINTNETAPP-337: Change color from CYAN to gray for end of rule.
 *   - IKMAIN-19799: Add padding on the right side for the status wrapper in the landing page list row.
 *   - Handle the back and search key press on alert dialogs in activities.
 *    
 *  v1.1.187 - Suren
 *   - IKMAIN-19589: Change the wording of the battery when clause.
 *   - IKMAIN-19598: Change list font sizes from 23sp to 22sp and also color of icons at places.
 *   
 *  v1.1.188 - Paramita
 *   - IKINTNETAPP-337 - Added a new gray to Colors.xml for the block status (End of Rule and Conflict)
 *   - Added extra logging to Debug table:
 *     Action : OnCreate
 *     Action : OnEdit
 *     Action : OnDelete 
 *     Suggested rule accepted
 *     Suggested rule rejected
 *     Sample rule accepted
 *     Manual rule : Active to Disabled
 *     Manual rule : Disabled to Active
 *     Automatic rule : Ready to disabled
 *     Automatic rule: Active to disabled
 *     All rules disabled
 *     Automatic Rule : Disabled to Ready
 *  
 *  v1.1.189 - Rohit
 *   - IKMAIN-20026 - Handle orientation changes for Suggestions Inbox activity
 *
 *  v1.1.190 - Rohit 
 *   - IKINTNETAPP-345 - Add Debug Table log messages when the user Accepts or Rejects Suggestions
 *    
 *  v1.1.191 - Paramita
 *   - IKMAIN-20172 - Image background needs to be grayed out for disabled actions and conditions (in the selection list)
 *    
 *  v1.1.192 - Suren
 *   - IKMAIN-21011: Change description of the discard dialog text as per CxD.
 *   - IKINTNETAPP-340: Change the write to debug viewer DB to a thread from an AsyncTask.
 *   
 *  v1.1.193 - Suren
 *   - IKMAIN-21294: Change text in the first time suggestions dialog displayed to user.
 *   - IKMAIN-19625: Change second line text displayed for disabled rules. 
 *   - IKMAIN-19615: Return true to hear the click in the onClick of the menu item of Rules Builder.
 *   
 *  v1.1.194 - Suren
 *   - IKMAIN-20085: Remove the background attribute in the title bar layout to show the blue color on selection.
 *   - IKMAIN-21348: Handle orientation changes for copy rule and display rule activities. 
 *   - IKMAIN-21069: Move the copying of a rule into a thread for better performance and UI behavior.  
 *
 *  v1.1.195 - Boby Iyer 
 *   - IKMAIN-20341: Fix Rule descriptions
 *                               Also make Night Time Battery Saver enabled out of the box.
 *
 *  v1.1.196 - Rohit
 *   - IKMAIN-19583: Remove android.intent.action.MAIN from Suggestions UI activities
 *   - IKMAIN-21691: Change String in settings screen
 *                              - Enable some critical logs in PRODUCTION_MODE = true
 *                              - Update PendingIntent in the notifications
 *                              - Kill Suggestion Dialog act on Pause
 *     
 *  v1.1.197 - Paramita
 *   - IKINTNETAPP-348: Refactoring Rules builder code
 *     
 *  v1.1.198 - Rahul
 *   - IKINTNETAPP-318: Shadow for scrollview color blocking some changes to pick background graphics
 *   - IKMAIN-21073: Audible selection issues in RulesBuilder Blocks layouts
 *   - IKINTNETAPP-347: HD display issues, using dpi instead of pixels
 *   
 *  v1.1.199 - Suren   
 *   - IKINTNETAPP-332: Enable the help changes to launch the placeholder page.
 *   - IKINTNETAPP-350: Modify strings to reflect Auto Pilot and other changes.
 *   - IKINTNETAPP-346: Update xhdpi media for HD phones (framework and titlebar)
 *   - IKMAIN-21687: Fix period in the string displayed in dialog displayed when launch application is selected.
 *   - IKMAIN-20864: Fix force close issue seen when deleting a rule (database lock issue)
 *   - IKINTNETAPP-351: Comment out strict mode code until further investigation.
 *   
 *  v.1.1.200 - Rohit
 *   - IKMAIN-22304: Add android:finishOnCloseSystemDialogs to SuggestionDialogActivity manifest.
 *  
 *  v1.1.201 - Suren
 *   - IKINTNETAPP-350: Modify strings to reflect Auto Pilot and other changes.
 *   - IKMAIN-22178: Update the about text to reflect Auto Pilot.
 *   - IKMAIN-22190: Fix the permission issue for the help page to be shown correctly.
 *   - Delete help.html file from the assets folder.
 *   - Delete the opt-in and opt-out layouts and related code.
 *   
 *  v1.1.202 - Suren
 *   - IKINTNETAPP-354: Changes to handle invisible rules especially night time battery saver rule.
 *   
 *  v1.1.203 - Rohit
 *   - Update notification logic, check Action table for new actions before showing notification.
 *   - Start landing page from Inbox only if launched from notification (external)
 *   
 *  v1.1.204 - Saral
 *   - Permission related changes
 *   
 *  v1.1.205 - Paramita
 *   - Location Consent Screen changes.
 *   
 *  v1.1.206 - Boby Iyer 
 *   - IKINTNETAPP-354 - Make Night Time Save invisible
 *                     - Add new rule to turn Data Sync on               
 *   - IKINTNETAPP-355 - Change Uri to launch Car Dock 
 *                     - Fix Time frame rules to remove extra ;end from VSENSOR intents
 *                     
 *  v1.1.207 - Suren & Paramita
 *   - IKMAIN-22125: Show selection background for rule icon and title bar in Puzzle Builder.
 *   - IKMAIN-22981: Fix the period missing in the About screen.
 *   - IKMAIN-21937: Fix force close due to NPE in Puzzle Builder.
 *   - IKINTNETAPP-312: Changed title bar to be editable only on long press of the title bar. 
 *   
 *  v1.1.208 - Suren
 *   - IKINTNETAPP-358: Replace Auto Pilot word with Smart Actions.
 *   - IKMAIN-23163: Replace WiFi with Wi-Fi in location consent dialog text.   
 *   
 *  v.1.1.209 - Rohit
 *   - IKMAIN-23337: Prohibit quick multi-clicks in Suggestion Inbox
 *   
 *  v.1.1.210 - Paramita
 *   - IKINTNETAPP-359: Change in Location Consent dialog flow.
 *   
 *  v1.1.211 - Suren
 *   - IKINTNETAPP-247: Fix the force close issue due to out of memory exception for bitmaps not recycled.
 *   - Comments from Paramita's changes v1.1.210 incorporated.
 *   - Added two additional icons to the icon selection grid.
 *   
 *  v1.1.212 - Rohit
 *   - IKMAIN-23961: Enable Location Backup and Restore
 *   
 *  v1.1.213 - Suren
 *   - IKINTNETAPP-373: Remove duplicated/unused media.
 *   - IKINTNETAPP-346: Update HD media for xhdpi devices.    
 *   
 *  v1.1.214 - Paramita
 *   - IKSTABLE6-767: Fixed Window Leak for progress dialog.
 *   
 *  v1.1.215 - Suren and Paramita
 *   - IKINTNETAPP-346: Update the pink HD media for framework. 
 *   - IKSTABLE6-1493: Hide the user friendly instructions for actions/trigger from the canvas only when atleast
 *   					one action or trigger block is present.
 *   
 *  v1.1.216 - Boby Iyer
 *   - IKMAIN-24179 : Add Action Names to RulesImporter.rules
 *   - Add com.motorola.contextual prefix to Location based DVSs
 *   
 *  v1.1.217 - Paramita Banik
 *   - IKSTABLE6-2046 Fixed FC in SmartRules.
 *
 *  v1.1.218 - Rohit
 *   - IKINTNETAPP-387: Suggestion Dialog RuleIcon scaling removed.
 *  
 *  v1.1.219 - Suren
 *   - IKSTABLE6-931: Delete HashUtil.java and rewrote it in Hash.java.
 *  
 *  v1.1.220 - Boby Iyer
 *   - IKINTNETAPP-379: Read rulesimporter.rules from /system/etc/rules.d if it exists
 * 
 *  v1.1.221 - Suren 
 *   - IKINTNETAPP-380: Hide Locations from Profile list if the ro to 
 *   					hide locations is set to true in system.prop file.
 *   
 *  v1.1.222 - Paramita
 *   - IKINTNETAPP-388 - Check for WiFi Sleep policy when showing location Consent dialog
 *   - IKINTNETAPP-381 - Disable Location trigger for Korea
 *   
 *  v1.1.223 - Suren
 *   - IKSTABLE6-2877: Avoid accidental key presses resulting in multiple popups in
 *   				   Check Status Activity.
 *   - Move the refresh list code from SuggestionsInboxActivity to DisplayRulesActivity. 
 *   
 *  v1.1.224 - Suren
 *   - IKINTNETAPP-400: Update ongoing rules notification accordingly for 
 *   					scenarios mentioned in the CR.
 *   - IKINTNETAPP-401: Remove the black border around the suggestion dialog on
 *   					HD devices and make the dialog transparent.
 *   - IKSTABLE6-2518: Fix force close due to bitmap recycle for delete rule
 *   				    and slider close. 
 *   
 *  v1.1.225 - Paramita
 *   - IKINTNETAPP-378: Disabled the "Add Rule" button if sample rule is not completely
 *                      configured.
 *                      
 *  v1.1.226 - Suren
 *   - IKSTABLE6-3540: Add logic to runOnUiThread for the thread created to copy
 *   				   the user selected rule and move the progress dialog dismiss
 *   				   to onPause().
 *   - IKSTABLE6-3633: Handle the back key and search button press on the dialog
 *   				   displayed by long press of an action or trigger block.
 *   - Change the Debug logs from internal to out in rules builder for them to be
 *   	seen on the check-in server.
 *   
 *  v1.1.227 - Boby Iyer
 *   - IKINTNETAPP-403 : Fix Icon for suggested rule
 *   
 *  v1.1.229 - Boby Iyer
 *   - IKINTNETAPP-414 : fix VSENSOR intent in rulesimporter.rules
 *
 *  v1.1.230 - Paramita
 *   - IKINTNETAPP-413 - Enable the "Add Rule" button if an unconfigured block has 
 *                       been deleted or disabled.
 *
 *  v1.1.231 - Suren
 *   - IKINTNETAPP-402 - Implement the new About and Landing Page screens.
 *   - IKINTNETAPP-416 - Change the text for manual rules to "Touch icon to enable"
 *   - IKINTNETAPP-417 - Fix bug in the landing page when fetching the action cursor
 *   					 to display error for an active rule.
 *   
 *  v1.1.232 - Suren
 *   - IKINTNETAPP-402 - Mix minor issues missed in earlier push for about screen changes. 
 *   
 *  v1.1.233 - Rahul
 *   - IKSTABLE6-1774  - Resolve MotionEvent suppress in case activity waiting for configure dialog box
 *   - IKSTABLE6-2088  - Fix layout to avoid more than 5 lines in Block description
 *   
 *  v1.1.234 - Rohit
 *   - IKINTNETAPP-407 - RulesImporter design logic updated to reduce no# of DB calls
 *   - IKINTNETAPP-426 - Rename Suggestion title to "Suggested Smart Actions"
 *   - Change to replace Sample with Suggestion added
 *   - Suggestion Title aligned to center
 *   
 *  v1.1.237 - Suren and Paramita
 *   - IKSTABLE6-3617: Force close issues fixed in Smart Rules (OOM, AsyncTask, Bitmap Recycle)
 *   - IKSTABLE6-5935: Add icon for quiet location to the icons dialog
 *   - IKINTNETAPP-405: Display error message when displaying empty triggers list.
 *   - IKINTNETAPP-415: Block text and color changed for not configured actions/triggers
 *   - Changes to support package name, version, video and progress spinner for help.
 *
 *	v1.1.238 - Boby Iyer
 *   - IKINTNETAPP-431 : Localization Changes
 *   
 *  v1.1.239 - Boby Iyer
 *   - IKINTNETAPP-432 : Fix for data turning off when modes are switched too soon
 *   
 *  v1.1.240 - Rahul
 *   - IKSTABLE6-6297: Patch for breakage out of previous fix IKSTABLE6-1774 in v1.1.233
 *   
 *  v1.1.241 - Boby Iyer
 *   - IKINTNETAPP-436 : Fix breakage on Turning off data
 *   - IKSTABLE6-7390 : Action Desc change
 *   - IKINTNETAPP-434 : Rule Desc change
 *   
 *  v1.1.242 - Suren
 *   - IKSTABLE6-7141: Do not close the DB if the build is Gingerbread or before. For
 *   				   Honeycomb and beyond open and close DB for every DB query. (This
 *   				   is a HACK to prevent the Force Closes due to database being locked).
 *   
 *  v1.1.243 - Suren
 *   - IKSTABLE6-8891: Read from Flex Shared Preference instead of SystemProperties to show
 *   					Locations or not. 
 *   
 *  v1.1.244 - Boby Iyer			
 *   - IKSTABLE6-7275: Read Condition Names from db. Populate XML with condition names
 *   
 *  v1.1.245 - Suren 
 *   - IKINTNETAPP-438: Add an interface for the Build.VERSION_CODES to prevent compile failure
 *   					on pre-gingerbread builds.
 *   - IKSTABLE6-9738: Add "My Rules" divider to Landing Page.
 *   - Enable LOG_INFO for all builds and change few LOG_INFO to LOG_DEBUG. 
 *     
 *  v1.1.246 - Paramita
 *   - IKINTNETAPP-442: Fixed breakage introduced with trigger names in Smart rules v1.1.244
 *   - IKINTNETAPP-441: Don't allow user to save a rule if there is a connected unconfigured action.
 *  
 *  v1.1.247 - Rahul
 *   - IKSTABLE6-9700: Remove first time in-app location consent
 *   - IKSTABLE6-10556: added location and GPS triggers/actions in the disable mapping for actions/conditions 
 *   
 *   *  v1.1.248 - Boby Iyer			
 *   - IKSTABLE6-10756: Remove Wifi from Location related sample rules
 *
 *  v1.1.249 - Rahul
 *   - IKMAIN-26789 Fix for latent issue in baseline, causing force close on building Action/Condition list
 *   
 *  v1.1.250 - Suren, Rahul and Boby
 *   - IKSTABLE6-13009 - Bunch of changes as per CxD
 *   	- Show error on Landing Page for Location based rules if shown in Puzzle Builder.
 *   	- Add virtual view to Provider to return the count of active location based rules.
 *   	- Updates to sample and suggested rules.
 *   	- Display Location and Wi-Fi dialog if location block is part of a rule only for 
 *   		the first time (either from scratch or sample or suggestion).
 *   	- Display different error dialogs for location error in puzzle builder based on
 *   		a combination of Location consent and Wi-Fi state.
 *   
 *    v1.1.251 - Boby Iyer			
 *   - IKSTABLE6-13460 : XML Changes to add Motion Sensor Trigger
 *   
 *    v1.1.252 - Rahul Pal
 *   - IKSTABLE6-13646: Minor change to fill missed requirements for Loc/Wifi changes part of IKSTABLE6-13009
 *
 *	  v1.1.253 - Boby Iyer
 *   - IKSTABLE6-13606 :  Suggested sleep rule will not work if accepted from suggestion dialog
 *   					 
 */

/** Smart Profile Change History
 * v1.1.0 - Suren
 * 	- Initial Creation.
 * 	- Added logic to infer a location (home and menaingful location) and display the notification to the user.
 * 	- On selecting the notification dialogs displayed to the user to set a rule.
 *
 * v1.1.1 - Suren
 * 	- Removed the widget and moved the notification code into the Smart Profile app.
 *
 * v1.1.2 - Suren
 * 	- Moved the Locations List Activity code into Smart Profile.
 * 	- Added dialog to invoke rules compose when user adds a POITag to a location.
 *
 * v1.1.3 - Suren
 *  - Added new activity to display all locations.
 *  - Modified LocationsListActicity to display only locations with a PoiTag.
 *  - Menu option added to launch the activity to display all locations list.
 *
 * v1.1.4 - Suren
 *  - Modified the receiever to take a bundle and read from it for the values lat, lng etc..
 *  - Passing additional extras lat, lng etc... in the pending intent to notification manager.
 *  - Writing a record to new table names poi in Location Sensor app database when location is
 *    inferred and user selects yes to proceed and also when user manually tags a location as
 *    meaningful location from the list of locations.
 *
 * v1.1.5 - Suren
 *  - Added intent extra to identify that Smart Profile App launched Rules Composer.
 *  - Removed AllLocationsListActivity and modified LocationsListActivity to display the list of
 *  	all meaningful locations.
 *  - Created new activity PoiLocationsListActivity to display list of POI tagged locations. Menu
 *  	option to launch the LocationsListActivity will be available. Application launcher points to
 *  	this activity now.
 *  - PoiLocationsListActivity can also be launched by Rules Composer and display checkboxes
 *  	and a Save and Cancel button for the user to select. Menu option to show all locations
 *      will be disabled in this type of launch.
 * 	- Fixed bug that was passing the poitag instead of the actual address to the LocationsDetailActivity
 * 		and in turn to the Map Activity when trying to geocode using the address.
 * 	- Modified the list row layout to include the checkbox and the list layout to include the Save and
 * 		Cancel button.
 *
 * v1.1.6 - Suren
 * 	- Added a CreateDvs class
 *  - Added code to create a Dynamic VSM before launching Rules Composer from SmartProfileActivity and
 *  	LocationsListActivity class.
 *  - Removed code to check current location with Location Sensor app and write to checkin server.
 *
 * v1.1.7 - Suren
 *  - Added code for Time Sensor and Battery Sensor. Also updated the code to create dynamic VSM for
 *  	Time Sensor and Battery Sensor.
 *
 * v1.1.8 - Bhavana
 *  - Added code for AirPlane, HeadSet, Sync, Charging, Storage sensors. Also changed the  CreateDynamicVirtualSensor
 *    mechanism to inheritance based.
 *
 * v1.1.9 - Ashok Oliver
 *  - Enhancements to the  Time Frame Sensors.
 *
 *  - Notes.
 *     1. Need a reboot after apk installation to preload the time frames
 *     2. Use "Time Manager" to create/edit/delete time frames
 *     3. Constraints
 *        1. Time frame names are unique
 *        2. Start Time should be less than End Time
 *        3. While Editing a Time Frame, its name cannot be changed.
 *        4. Future optimizations
 *        5. Register for alarm intents only when there is a sensor created for this time frame
 *
 *     4. To Do
 *        1. Support time frame across days.
 *
 * v1.1.10 - Bhavana
 *   - Checked in wificonnected, wifidevices, btconnected, btdevices, timezone sensors.
 *   - Not enabled in AndroidManifest since more changes are expected
 *   - Checked in "VSENSOR" extra addition to the intent back from Preconditions - airplane,
 *     storage, sync, timezone, headset
 *
 * v1.1.11 - Ashok Oliver
 *  Checked in "VSENSOR" extra addition to the intent back from Preconditions - TimeFrame
 *
 * v1.1.12 - Suren
 * 	- Added logic to re-query the aggregate URI from Location Sensor and use the data in the cursor
 * 		that matches the poi tag entered by the user when writing to poi table.
 *
 *  v1.1.13 - Ashok Oliver
 *  - Added support for time frames across days. Time frames like 11PM - 4AM are now supported. 4AM of next day is assumed
 *
 *  v1.1.14 - Suren
 *   - Merged changes for latest VSM and work with Paramita's Puzzle Builder app for Location, Battery, Airplane and Headset
 *   	sensors.
 *   - Added metadata for publisher key in the manifest file for Location, Battery, Airplane and Headset sensors.
 *   - Added intent action for launching Locations list from Smart Rules.
 *
 *  v1.1.15 - Suren
 *   - Missed a file for location during merge in v1.1.14.
 *
 *  v1.1.16 - Suren
 *   - Modified the list row to display the second line with the address and the checkbox to the right.
 *   - Commented out DVS creation for BT, Charging, Storage, Sync, Time, Time Zone, Wi-Fi Connect and Wi-Fi
 *   	devices sensors.
 *   - Added icons and publisher key metadata to the Android Manifest file.
 *
 *  v1.1.17 - Bhavana
 *   - Correction for the compile issue of Wifi and Charger DVS
 *   - Enable Wificonnectivity, Wifinetworks, BTConnectivity, BTDevices, Missed Call in Manifest
 *   - Missed call working for 1 contact
 *
 *  v1.1.18 - Ashok
 *   - Added changes to pass publisher key for time frame sensor
 *
 *  v1.1.19 - Bhavana
 *   - Changes to convert the pre-conditions to condition builder framework
 *
 *	v1.1.20 - Suren
 *	 - Code added to add locations from the POI List.
 *	 - Changes to convert the XML String to support new VSM changes.
 *   - Removed launchers from the Androud Manifest file.
 *
 *  v1.1.20 - Vignesh
 *   - Headset Sensor added
 *   - Modified Battery sensor
 *
 *  v1.1.21 - Vignesh
 *   - One liner change in Constants.java for Battery sensor
 *
 *  v1.1.22 - Ashok
 *   - Changes to allow edit of time frames from the list view
 *
 *  v1.1.23 - Suren
 *   - Changes to write the inferred location to location sensor database and not show up the
 *   	notification to the user. (Changes made as per discussion with Venki and Vignesh).
 *
 *  v1.1.24 - Ashok
 *   - Changes to read Wifi, BT & Gps state and broadcast it with the newly inferred meaningful
 *   	location
 *
 *  v1.1.25 - Ashok
 *   - Added additional pre-load time frames and additional sanity checks while creating time frames
 *
 *  v1.1.26 - Saral
 *   - @Override for onTouch(...) in the TimeFrameCheckListActivity.java is removed.
 *
 *  v1.1.27 - Ashok
 *   - Made the time frame rule intents to be sticky. Intents fired by AM will now be received by
 *    a local receiver and a corresponding sticky intent will be broadcasted, which will be used for
 *    the rule based VS
 *
 *  v1.1.28 - Suren
 *   - Added code for Edit Locations Activity. Selecting the POI Location will show the location
 *   	details and edit functionality.
 *   - Deleted the + sign icons and replaced with the icon from Tom.
 *
 *  v1.1.29 - Suren
 *   - Added more functionality for the Edit Locations Activity.
 *   - Cleaned up code as well as Constants.java.
 *
 *  v1.1.30 - Ashok
 *  - Added the broadcast receiver in manifest, since the dynamic receiver seems to have issues
 *    in DSM
 *
 *  v1.1.31 - Ashok
 *  - Made time frame names, editable. Every time-frame will have a unique internal name which
 *    will be  used in the rules for creating the dvs. Enhanced logic to fine tune the intent
 *    registration so that if both start time and end time is already over for the day, the
 *    intents wont be registered for the day.
 *
 *  v1.1.32 - Suren
 *   - Modifed the edit location layout as per discussion with Daniel and also changed the activity.
 *
 *  v1.1.33 - Suren
 *   - Added the new weighted layout for Edit Location and removed the old scroll layout.
 *   - In the POI locations list launched from puzzle builder made the complete row clickable and not
 *   	just the checkbox.
 *   - Added additional constants for puzzle builder launch.
 *   - Added try catch around a crash seen in Timezone code.
 *
 *  v1.1.34 - Suren
 *   - Added code to ignore if the tag was an empty string when checking is the location is a meaningful
 *   	location or not.
 *   - Added delete option for the POI List so it can be deleted from the poi table and the poi tag be
 *   	reset in the loctime table.
 *   - Added a reset option to the long press for the all locations list even if there might not be a tag.
 *
 *  v1.1.35 - Suren
 *   - Made some Look 10 changes to the UI.
 *
 *  v1.1.36 - Suren
 *   - Some more additional changes for Look 10.
 *
 *  v1.1.37 - Suren
 *   - Title bar changes for Look 10.
 *   - Cleaned up drawables folder.
 *
 *  v1.1.38 - Suren
 *   - Further Look 10 changes.
 *
 *	v1.1.39 - Suren
 *	 - Changed the location list row to be one line for title and address with elipsis at the end and also
 *		made sure the text does not overlap with the checkbox when shown.
 *	 - Changed Timeframes to Time Frames.
 *	 - Changed android:windowNoTitle item in styles.xml from false to true so that title is seen in the required
 *		activities. Activities that do not need a title have to set the requestWindowFeature(Window.FEATURE_NO_TITLE)
 *		in their code.
 *
 *	v1.1.40 - Suren
 *   - Incorporated part 1 of review comments from Craig for Location code.
 *
 *  v1.1.41 - Suren
 *   - Use the new location_list_row.xml layout from Tom. Also now All Locations Activity will hide the first line and
 *   	use the second line to show the text as it is two lines and the first lie was only one line.
 *
 *	v1.1.42 - Suren
 *	 - Changed layouts to use the Look 10 action bar.
 *
 *	v1.1.43 - Suren
 *   - Fix issue # 92 related to edit location not saving the new name.
 *   - Incorporated part 2 of comments from code review for location.
 *
 *  v1.1.44 - Suren
 *   - Replaced text displayed and returned from Smart Profile for pre-conditions.
 *   - Added icons for dialog and updated with the latest icons from CxD.
 *
 *  v1.1.45 - Suren
 *   - Replaced icons with the new ones from Daniel (scaled down from 90x90 to 72x72).
 *
 *  v1.1.46 - Suren
 *   - Added shadow to the list view.
 *
 *  v1.1.47 - Suren
 *   - Added context menu to show Edit and Delete for long press on locations list when
 *   	checkbox is shown or not.
 *
 *  v1.1.48 - Suren
 *   - Added new layout for edit location.
 *   - Updated the POI location list to show address only if the name is the same as the address.
 *
 *  v1.1.49 - Suren
 *   - Added the Done button to the edit text box in Edit Location activity.
 *   - Logic added to enable the save button when the edit box is selected in the Edit Location activity.
 *   - When computing if a location is a meaningful one or not check for accuracy of < 100 ms for the locations
 *   	before proceeding further.
 *   - Change the long click menu to a Contextual Menu and not an alert dialog in POILocationsListActivity.
 *
 *  v1.1.50 - Suren
 *   - Redesigned the Location prediction and geocoding of address to a thread.
 *   - Implemented comments from earlier code review.
 *
 *  v1.1.51 - Suren
 *   - Layouts edited to show the pressed state and also the grey list background.
 *   - Redesigned the location details activity to be a preference activity and also the layout used.
 *
 *  v1.1.52 - Suren
 *   - Layouts modified to show Done in the keypad and the first letter to start with uppercase.
 *   - Time Frames changed to Timeframes as per CxD suggestion.
 *   - A IntentService is created to handle the location change and not create a thread in the
 *   	broadcast receiver.
 *   - Minor comments from previous review fixed.
 *
 *  v1.1.53 - Suren
 *   - Fixed the bugs reported by findbugs tool for location related code.
 *   - Reverted the set tags to use the nested view.getParent() code as the view.getRootView().findViewById()
 *   	was having weird behaviors.
 *
 *  v1.1.54 - Suren
 *   - Updated media from CxD.
 *   - Fixed findbugs in location code.
 *   - Replaced the long XML strings to a StringBuilder code.
 *
 *  v1.1.55 - Suren
 *   - Added uses permission for location sensor.
 *
 *  v1.1.56 - Suren
 *   - Changes to Location related activities and service as per CxD redesign.
 *   - Changes to fix Vsensor string issue in Time Sensor code.
 *
 *  v1.1.57 - Suren
 *   - Review comments from v1.1.56 incorporated.
 *
 *  v1.1.58 - Suren
 *   - Display test changed for Missed Calls, Incoming Calls, Bluetooth Connection, Wi-Fi Connection.
 *   - Fixed the " or " string split reading in Locations List.
 *
 *  v1.1.59 - Suren
 *   - Location Sensor DB tables related operations moved to a common file.
 *   - Fixed crashes when there is no poiType in the DB for a POI.
 *
 *  v1.1.60 - Vignesh Karthik-a21034
 *   - Change default timeframes
 *   - Register with Alarm Manager for every VSM_INIT_COMPLETE in timesensors
 *
 *  v1.1.61 - Suren
 *   - Updated new media from CxD.
 *   - Added second line to the title bar for Locations and also fixed the list display issue when the name
 *   	and address are the same.
 *   - Minor layout issues fixed for Locations.
 *
 *  v1.1.62 - Suren
 *   - IKINTNETAPP-225: Added xhdpi and mdpi media. Cleaned up common media and
 *   	moved them to drawables folder.
 *
 *  v1.1.63 - Suren
 *   - IKINTNETAPP-198: Fixed few Klocwork Issues
 *   - IKMAIN-16151: Show error when user tries to delete a location that is associated with a rule.
 *   - IKINTNETAPP-187: Implement V1.1. Location Picker Changes
 *   	- New layout for Locations List with the list divider between text and check box.
 *   	- Make text and check box separate touch targets.
 *
 *  v1.1.64 - Vignesh karthik-a21034
 *   - Fixes in Missed Call sensor - IKINTNETAPP-208
 *
 *  v1.1.65 - Suren
 *   - IKINTNETAPP-198: Fixed Klocwork Issues in Location
 *   - IKINTNETAPP-187: Implement V1.1. Location Picker Changes
 *   	- When user clicks the checkbox of a suggested location - launch the edit location to confirm.
 *   	- Store the state and restore it when the orientation changes on the list.
 *   	- Refactored onCreate() code.
 *   	- Removed unused list view item click code.
 *   	- When cancel is selected and no location was selected do not build the strings.
 *
 *  v1.1.66 - Suren
 *   - Turned PRODUCTION_MODE to true in Constants.java file.
 *
 *  v1.1.67 - Vignesh karthik - a21034
 *   - Anymore, it would not be possible to delete a timeframe if used by any Smart rule
 *
 *  v1.1.68 - Suren
 *   - IKINTNETAPP-251: Fix the logic inside the locations list when returning to Puzzle Builder.
 *   - IKINTNETAPP-252: Edit Location layout modified to put a min height for the Change Location layout.
 *
 *  v1.1.69 - Suren
 *   - IKMAIN-17428: Change the app icon to the same one as Smart Rules.
 *   - Remove unused variable pointed out via findbugs.
 *   
 *  v1.1.70 - Suren
 *   - IKINTNETAPP-306: Fix force close in Locations due to changes in Aloqa and also to escape a '
 *   					character when doing a string query. 
 *
 *  v1.1.71 - Suren
 *   - IKMAIN-17980: Fix space issue in timeframes days of the week row.
 *   - Fix logging statements that do not have an if(LOG_XXXX)
 *   
 *  v1.1.72 - Suren
 *   - IKMAIN-18063: Revert changes made in IKMAIN-17980 to add space in trimefames days of the week row. 
 *   
 *  v1.1.73 - Suren
 *   - IKINTNETAPP-290: Change lists text size as per CxD specifications.
 *   - IKINTNETAPP-318: Updates as per CxD recommendations.
 *   		- Add updated drawables
 *   		- Replace the icon used for incoming calls trigger.
 *   		- Increase the min-height for Change Location row in Edit Location layout.
 *   		- Change the title bar shadow values so that the shadow is up and not down. 
 *   
 *  v1.1.74 - Suren
 *   - IKINTNETAPP-290: Fix text size as per CxD Specifications
 *   - Added StrictMode checker function in the Utils and called from PoiLocationsListActivity.   
 *   
 *  v1.1.75 - Suren
 *   - IKINTNETAPP-324: Fix breakage in Froyo due to addtion of StrictMode code. 
 *  
 *  v1.1.76 - Suren
 *   - IKINTNETAPP-324: Modify the strict() handler in Util.java to directly refer to 
 *   					android.os.StrictMode and remove the import. 
 *  
  
 *  v1.1.77 - Suren
 *   - IKINTNETAPP-335: Update the pin drawable that is used to point a location on a map. 
 *   
 *   v1.1.78 - Boby Iyer
 *   -  IKCTXTAW-312 : Broadcast Intent format changed.
 * 
 *  v1.1.79 - Suren
 *   - IKMAIN-19598: Change list font sizes from 23sp to 22sp. 
 *   
 *   v1.1.80 - Boby Iyer
 *   - IKINTNETAPP-343: Change Night Time time frame to end at random time between 4 and 5 in the morning
 *   
 *   v1.1.81 - Suren
 *    - IKMAIN-21617: Fix the no meaningful location string.
 *    - IKINTNETAPP-346: Update xhdpi media for action bar and title bar. 
 *    
 *   v1.1.82 - Suren
 *    - IKINTNETAPP-351: Comment out strict mode code until further investigation.
 *    
 *   v1.1.83 - Suren
 *    - IKMAIN-22177: Update drawables for incoming and missed calls. 
 *    - IKINTNETAPP-350: Update string as per discussions with CxD. 
 *
 *   v1.1.85 - Rohit
 *    - IKMAIN-23961: Enable Location Backup and Restore
 *
 *   v1.1.86 - Boby Iyer and Suren
 *    - IKINTNETAPP-360: Change in location inference logic
 *    
 *   v1.1.87 - Suren
 *    - IKINTNETAPP-373: Remove duplicated/unused media.
 *    - IKINTNETAPP-346: Update HD media for xhdpi devices. 
 *    
 *   v1.1.88 - Suren
 *    - IKINTNETAPP-374: Mark the newly added location as checked in the locations list.  
 *    
 *   v1.1.89 - Suren
 *    - IKINTNETAPP-346: Update the pink HD media for framework. 
 *    
 *   v1.1.90 - Suren
 *    - IKINTNETAPP-375: Add com.motorola.contextual. to the sensors created for Location based rules.
 *
 *   v1.1.91 - Suren
 *    - IKINTNETAPP-382: Add com.motorola.contextual. to the sensors created for Location based inference rules.
 *
 *   v1.1.92 - Suren
 *    - IKINTNETAPP-389: Replace blank spaces with %20 and remove non-alphanumeric
 *    					 characters when creating location based sensors.
 *    
 *   v1.1.93 - Suren
 *    - IKSTABLE6-3642: Fix padding issues in Edit Location layout. 
 *    
 *   v1.1.94 - Suren
 *    - IKSTABLE6-4701: Fix the location edit issue for a location used in an active rule.
 *    		- Added logic to use unique poi names and not the address.
 *    		- Remove unused logic and files. 
 *    
 *   v1.1.95 - Suren
 *    - IKSTABLE6-6159: Handle use case of checked locations with the same name (caused
 *    					the ANR of arrayoutofbounds exception when accessing poi names list).
 *
 *   v1.1.96 - Rohit
 *    - Location name change being updated in Rule ConditionTable
 *    - Projection added to most of the DB Queries
 *  
 *   v1.1.97 - Suren
 *    - IKSTABLE6-6621: Fix empty name for location when selected and in dialog title.
 *    		- Disable save button when edit location is launched.
 *    		- Enable save button only when user either edits the name or changes location.
 *     		- When save button selected check if the edited name is null or empty and 
 *     			store approrpiately.
 *     		- Refactored code to make onCreate() reasonably longer.  
 *     
 *   v1.1.98 - Suren
 *    - IKSTABLE6-7013: Check for blank name when adding a location from Aloqa.  
 *    
 *   v1.1.99 - Suren
 *    - IKINTNETAPP-362: Use StringUtil.IsEmpty(String) instead of String.isEmpty().
 *    - IKSTABLE6-7455: Avoid adding duplicate locations into the Location Sensor DB.
 *    - IKSTABLE6-7863: Save button disabled on edit location screen when name is empty.
 *       
 *   v1.1.100 - Suren
 *    - IKSTABLE6-11377: Fix bunch of issues related to Location
 *    		- Remove title from AndroidManifest for EditLocationActivity 
 *    		- Set the EditLocationActivity to not show title bar.
 *    		- Use ConditionDesc column to update the new location name in the DB.
 *     
 *   v1.1.101 - Suren
 *    - IKSTABLE6-13009: Change battery level from 20 to 25.
 *
 */

/** Inference Manager Change History
 * 
 * v1.1.0 - Ashok Oliver
 *     - Initial Creation. 
 *     - Initial version of Inference manager. Separate services are used for identifying 
 *       missed calls and outgoing sms. These will be changed to android based intents later   
 *     - A test notification widget is used for testing purpose. This will be changed to use 
 *       the SmartRule Widget, which will be developed by Suren.
 *       
 * v1.1.1 - Ashok Oliver
 *     - Integrated with Condition builder 
 *     - Eliminated the dependency on CallLogs service to detect missed calls 
 *     
 * v1.1.2 - Ashok Oliver
 *     - Removed unwanted code
 *     
 * v1.1.3 - Ashok Oliver
 *     - Inference logic enhancements  
 *     
 * v1.1.4 - Vignesh karthik
 *     - Inference baseline for Meaningful location related
 *  
 * v2.0 - Vignesh karthik-a21034
 * 	   - Capable of parameter Inference
 * 	   - Addressed problems while inferring rules with * as parameters
 * 
 * v2.1 - Vignesh karthik-a21034
 *     - Lots of cleanup, fresh RTI, fresh RTS
 *     
 * v2.2 - Vignesh karthik-a21034
 *     - All rules moved to new XML format
 *     - .next files maintained in database instead of flat file system
 *     - Removed sleep instead intent handlers by listening to intents from Rules Importer
 *     - Handle new intents
 *     
 * v2.3 - Vignesh karthik-a21034
 *     - Tighter coupling in InferenceTable and NextTable functions
 *
 * v2.4 - Vignesh karthik-a21034
 *      - Few null checks for IKINTNETAPP-235
 *      
 * v2.5 - Vignesh karthik-a21034
 *      - Fix Klocwork issues
 *      
 * v2.6 - Vignesh karthik-a21034
 *      - Support to display contact name in suggestions (missed call and sms)
 *      
 * v2.7 - Vignesh karthik-a21034
 *      - 1 out of 2 persistent services removed
 *      
 * v2.71 - Vignesh karthik-a21034
 *      - The last running persistent service removed
 *
 * v2.72 - Vignesh karthik-a21034 - IKINTNETAPP-310
 *      - Support to display contact name in suggestions (missed call and sms), but inside puzzle builder
 *      - Support updation of next content during updates via server
 *      - Working support of RTI reset in case of updation
 *      
 * v2.8 - Vignesh karthik-a21034 
 *      - Disable sensors when not required
 */
public class LegacyChangeHistory {}
