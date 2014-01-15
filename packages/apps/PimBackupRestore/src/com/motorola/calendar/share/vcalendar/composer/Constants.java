/*
 * Copyright (C) 2008, Motorola, Inc,
 * All Rights Reserved
 * Class name: Constants.java
 * Description: Please see the class comment.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 23-Jun-2008    e12128       Create header
 **********************************************************
 */

package com.motorola.calendar.share.vcalendar.composer;

/// ---- BEGIN, Remove below for T1
//import com.motorola.blur.common.simpledom.Node;
//import com.motorola.blur.util.mime.MimeConstants;
/// ---- END

/**
 * This interface defines a number of useful constants that are used across
 * various ActiveSync packages and classes. Some are very general, such as
 * MIME types, and others are more specific to ActiveSync.
 * @author pcockerell
 *
 */
public interface Constants {

/// ---- BEGIN, Remove below for T1
//    /** Type code for standard XML 1.0 processing. */
//    int XML_TYPE_XML10 = Node.XML_TYPE_XML10;
//    /** Type code for standard XML 1.0 processing, with formatting for output. */
//    int XML_TYPE_XML10_FORMATTED = Node.XML_TYPE_XML10_FORMATTED;
//    /** Type code for WbXML binary encoded processing. */
//    int XML_TYPE_WBXML = Node.XML_TYPE_WBXML;
/// ---- END

    /** The MIME type for a general stream of bytes. */
    String MIME_TYPE_OCTET_STREAM = "application/octet-stream";
    /** The MIME type for an RFC822 mail message. */
    String MIME_TYPE_RFC822 = "message/rfc822";
    /** The MIME type for a wbXML-encoded XML file used in ActiveSync. */
    String MIME_TYPE_WBXML = "application/vnd.ms-sync.wbxml";
    /** The MIME type for a wbXML-encoded XML file used in ActiveSync 12.1 and above. */
    String MIME_TYPE_WBXML_121 = "application/vnd.ms-sync";

    /** The value for the Cmd URL parameter in the GetAttachment command. */
    String ACTIVESYNC_CMD_GET_ATTACHMENT = "GetAttachment";
    /** The value for the Cmd URL parameter in the SendMail command. */
    String ACTIVESYNC_CMD_SEND_MAIL = "SendMail";
    /** The value for the Cmd URL parameter in the SmartForward command. */
    String ACTIVESYNC_CMD_SMART_FORWARD = "SmartForward";
    /** The value for the Cmd URL parameter in the SmartReply command. */
    String ACTIVESYNC_CMD_SMART_REPLY = "SmartReply";

/// ---- BEGIN, Remove below for T1
//    /** The name Java codecs expect for UTI-* encoding. */
//    String ENCODING_UTF_8 = MimeConstants.UTF_8;
/// ---- END

    /** The name of of the Add action element in Sync commands. */
    String EDIT_TYPE_ADD = "AsAdd";
    /** The name of of the Delete action element in Sync commands. */
    String EDIT_TYPE_DELETE = "AsDelete";
    /** The name of of the Fetch action element in Sync commands. */
    String EDIT_TYPE_FETCH = "AsFetch";
    /** The name of of the Change action element in Sync commands. */
    String EDIT_TYPE_CHANGE = "AsChange";
    /** The name of of the SoftDelete action element in Sync commands. */
    String EDIT_TYPE_SOFT_DELETE = "AsSoftDelete";

    /** The name of of the Add action element in FolderSync commands. */
    String EDIT_TYPE_FOLDER_ADD = "FhAdd";
    /** The name of of the Delete action element in FolderSync commands. */
    String EDIT_TYPE_FOLDER_DELETE = "FhDelete";
    /** The name of of the Update action element in FolderSync commands. */
    String EDIT_TYPE_FOLDER_UPDATE = "FhUpdate";

    /** The status element value of "OK" returned by most, but not all, ActiveSync commands. */
    int STATUS_OK = 1;

    /** The status element value of "OK" returned the FolderMove ActiveSync command. */
    int MV_STATUS_OK = 3;

    /** The status element value for "folder not found" for the FolderDelete command. */
    int FD_STATUS_NOT_FOUND = 4;

    /** The status element value for "invalid sync key" for the FolderSync command. */
    int FS_STATUS_TEMP_SERVER_ISSUE1 = 6;
    int FS_STATUS_ACCESS_DENIED = 7;
    int FS_STATUS_TIMED_OUT = 8;
    int FS_STATUS_BAD_SYNC_KEY = 9;
    int FS_STATUS_MALFORMED_REQUEST = 10;
    int FS_STATUS_TEMP_SERVER_ISSUE2 = 11;
    int FS_STATUS_UNKNOWN_ERROR = 12;

    /** The status element value for "folder doesn't exist" for the FolderUpdate command. */
    int FU_STATUS_ALREADY_EXISTS = 2;
    /** The status element value for "folder is special (In Box etc.)" for the FolderUpdate command. */
    int FU_STATUS_SPECIAL_FOLDER = 3;
    /** The status element value for "folder not found" for the FolderUpdate command. */
    int FU_STATUS_NOT_FOUND = 4;
    /** The status element value for "new parent folder not found" for the FolderUpdate command. */
    int FU_STATUS_PARENT_NOT_FOUND = 5;

    /** The status returned if there is no policy of the requested type. */
    int PV_STATUS_NO_POLICY = 2;

    /** The status element value for "invalid sync key" for the Sync command. */
    int SN_STATUS_BAD_SYNC_KEY = 3;

    /** The status element value for "protocol error" for the Sync command. */
    int SN_STATUS_PROTOCOL_ERROR = 4;

    /** The status send to the server if we conform to at least the PIN policy. */
    int PV_STATUS_CLIENT_PARTIAL_SUCCESS = 2;

    /** The status element value for a (possibly temporary) server configuration error. */
    int SN_STATUS_TEMP_SERVER_CONFIG_ERROR = 5;

    /** The status element value for "conflict" for the Sync command. */
    int SN_STATUS_CONFLICT = 7;

    /** The status element value for "not found" for the Sync command. */
    int SN_STATUS_NOT_FOUND = 8;

    /** The status element value for "out of space for account" for the Sync command. */
    int SN_STATUS_OUT_OF_SPACE = 9;

    /** The status element value for "folder hierarchy has changed" for the Sync command.
     * Need to resync the folder tree. */
    int SN_STATUS_HIERARCHY_CHANGED = 12;

    /** A value that doesn't correspond to any valid Ping command Status element value. */
    int PING_STATUS_NONE = 0;
    /** The Status element value for "timeout" for the Ping ActiveSync command. */
    int PING_STATUS_TIMEOUT = 1;
    /** The Status element value for "folders changed" for the Ping ActiveSync command. */
    int PING_STATUS_FOLDERS_CHANGED = 2;
    /** The Status element value for "missing XML element" for the Ping ActiveSync command. */
    int PING_STATUS_MISSING_ELEMENT = 3;
    /** The Status element value a general wbXML for the Ping ActiveSync command. */
    int PING_STATUS_GENERAL_ERROR = 4;
    /** The Status element value for "heartbeat out of range" for the Ping ActiveSync command. */
    int PING_STATUS_BAD_HEARTBEAT_INTERVAL = 5;
    /** The Status element value for "too many folders specified" for the Ping ActiveSync command. */
    int PING_STATUS_TOO_MANY_FOLDERS = 6;
    /** The Status element value for "need to do a FolderSync" for the Ping ActiveSync command. */
    int PING_STATUS_NEED_FOLDERSYNC = 7;
    /** The Status element value for "error so retry" for the Ping ActiveSync command. */
    int PING_STATUS_RETRY = 8;

    int FOLDER_TYPE_ROOT = 0;
    int FOLDER_TYPE_USER_CREATED_GENERIC = 1;
    int FOLDER_TYPE_INBOX = 2;
    int FOLDER_TYPE_DRAFTS = 3;
    int FOLDER_TYPE_DELETED_ITEMS = 4;
    int FOLDER_TYPE_SENT_ITEMS = 5;
    int FOLDER_TYPE_OUTBOX = 6;
    int FOLDER_TYPE_TASKS = 7;
    int FOLDER_TYPE_CALENDAR = 8;
    int FOLDER_TYPE_CONTACTS = 9;
    int FOLDER_TYPE_NOTES = 10;
    int FOLDER_TYPE_JOURNAL = 11;
    int FOLDER_TYPE_USER_CREATED_EMAIL = 12;
    int FOLDER_TYPE_USER_CREATED_CALENDAR = 13;
    int FOLDER_TYPE_USER_CREATED_CONTACTS = 14;
    int FOLDER_TYPE_USER_CREATED_TASKS = 15;

    /** If bit n is sent in this value, then folder type n is a system folder. */
    int FOLDER_MASK_SYSTEM = (1 << FOLDER_TYPE_ROOT) | (1 << FOLDER_TYPE_INBOX)
            | (1 << FOLDER_TYPE_DRAFTS) | (1 << FOLDER_TYPE_DELETED_ITEMS)
            | (1 << FOLDER_TYPE_SENT_ITEMS) | (1 << FOLDER_TYPE_OUTBOX) | (1 << FOLDER_TYPE_TASKS)
            | (1 << FOLDER_TYPE_CALENDAR) | (1 << FOLDER_TYPE_CONTACTS) | (1 << FOLDER_TYPE_NOTES)
            | (1 << FOLDER_TYPE_JOURNAL);

    /** If bit n is sent in this value, then folder type n is an email folder. */
    int FOLDER_MASK_EMAIL = (1 << FOLDER_TYPE_INBOX) | (1 << FOLDER_TYPE_DRAFTS)
            | (1 << FOLDER_TYPE_DELETED_ITEMS) | (1 << FOLDER_TYPE_SENT_ITEMS)
            | (1 << FOLDER_TYPE_OUTBOX) | (1 << FOLDER_TYPE_USER_CREATED_EMAIL);

    /** If bit n is sent in this value, then folder type n is contacts folder. */
    int FOLDER_MASK_CONTACTS = (1 << FOLDER_TYPE_CONTACTS)
            | (1 << FOLDER_TYPE_USER_CREATED_CONTACTS);

    /** If bit n is sent in this value, then folder type n is calendar folder. */
    int FOLDER_MASK_CALENDAR = (1 << FOLDER_TYPE_CALENDAR)
            | (1 << FOLDER_TYPE_USER_CREATED_CALENDAR);

    /** If bit n is sent in this value, then folder type n is tasks folder. */
    int FOLDER_MASK_TASKS = (1 << FOLDER_TYPE_TASKS)
            | (1 << FOLDER_TYPE_USER_CREATED_TASKS);

    /** If bit n is sent in this value, then folder type n is a user-created folder. */
    int FOLDER_MASK_USER_CREATED = (1 << FOLDER_TYPE_USER_CREATED_GENERIC)
            | (1 << FOLDER_TYPE_USER_CREATED_EMAIL) | (1 << FOLDER_TYPE_USER_CREATED_CALENDAR)
            | (1 << FOLDER_TYPE_USER_CREATED_CONTACTS) | (1 << FOLDER_TYPE_USER_CREATED_TASKS);

    /** A mask bit representing the Email class of folders. */
    int FOLDER_CLASS_MASK_EMAIL = 1;
    /** A mask bit representing the Contacts class of folders. */
    int FOLDER_CLASS_MASK_CONTACTS = 2;
    /** A mask bit representing the Calendar class of folders. */
    int FOLDER_CLASS_MASK_CALENDAR = 4;
    /** A mask bit representing the Tasks class of folders. */
    int FOLDER_CLASS_MASK_TASKS = 8;

    /** ActveSync Sync command &lt;Class> value for Calendar folders */
    String FOLDER_CLASS_CALENDAR = "Calendar";
    /** ActveSync Sync command &lt;Class> value for Contacts folders */
    String FOLDER_CLASS_CONTACTS = "Contacts";
    /** ActveSync Sync command &lt;Class> value for Email folders */
    String FOLDER_CLASS_EMAIL = "Email";
    /** ActveSync Sync command &lt;Class> value for Tasks folders */
    String FOLDER_CLASS_TASKS = "Tasks";
    /** Pseudo class for the Ping activity. */
    String FOLDER_CLASS_PING = "png";

    /** The string constant used to denote <code>true</code> in ActiveSync */
    String AS_TRUE = "1";

    /** The string constant used to denote <code>false</code> in ActiveSync */
    String AS_FALSE = "0";

    /** The ActiveSync SyncKey value for unsynced folders/folder trees. */
    String INITIAL_SYNCKEY = "0";

    /** The ActiveSync SyncKey value used to suppress syncs. */
    String IGNORE_SYNCKEY = "-1";

    /** The implicit external ID of the root folder, used by ActiveSync. */
    String ROOT_FOLDER_EXTERNAL_ID = "0";

    /** The format for the DATE_RECEIVED values */
    String STANDARD_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /** The email "importance" value for low-priority messages. */
    int EM_IMPORTANCE_LOW = 0;
    /** The email "importance" value for normal messages. */
    int EM_IMPORTANCE_NORMAL = 1;
    /** The email "importance" value for important messages. */
    int EM_IMPORTANCE_HIGH = 2;

    /**The Email Flag/Status element value for "Flag cleared" */
    int EM_FLAG_STATUS_CLEARED = 0;
    /**The Email Flag/Status element value for "Complete" */
    int EM_FLAG_STATUS_COMPLETE = 1;
    /**The Email Flag/Status element value for "Active" */
    int EM_FLAG_STATUS_ACTIVE = 2;

    /**The Email Flag/FlagType element value for follow-up */
    String EM_FLAG_TYPE_FOLLOWUP = "Follow up";

    /** The ActiveSync Sync FilterType for getting all messages. */
    int SYNC_FILTER_ALL = 0;
    /** The ActiveSync Sync FilterType for getting messages younger than one day. */
    int SYNC_FILTER_ONE_DAY = 1;
    /** The ActiveSync Sync FilterType for getting messages younger than three days. */
    int SYNC_FILTER_THREE_DAYS = 2;
    /** The ActiveSync Sync FilterType for getting messages younger than one week. */
    int SYNC_FILTER_ONE_WEEK = 3;
    /** The ActiveSync Sync FilterType for getting messages younger than two weeks. */
    int SYNC_FILTER_TWO_WEEKS = 4;
    /** The ActiveSync Sync FilterType for getting messages younger than one month. */
    int SYNC_FILTER_ONE_MONTH = 5;
    /** The internal Sync FilterType meaning "use the account default". */
    int SYNC_FILTER_DEFAULT = 7;
    /** The ActiveSync Sync FilterType for getting incomplete Tasks */
    int SYNC_FILTER_INCOMPLETE_TASKS = 8;

    /** The ActiveSync Truncation value to truncate the body to 0 bytes. */
    int TRUNCATION_BYTES_0 = 0;
    /** The ActiveSync Truncation value to truncate the body to 512 bytes. */
    int TRUNCATION_BYTES_512 = 1;
    /** The ActiveSync Truncation value to truncate the body to 1024 bytes. */
    int TRUNCATION_BYTES_1024 = 2;
    /** The ActiveSync Truncation value to truncate the body to 2048 bytes. */
    int TRUNCATION_BYTES_2048 = 3;
    /** The ActiveSync Truncation value to truncate the body to 5120 bytes. */
    int TRUNCATION_BYTES_5120 = 4;
    /** The ActiveSync Truncation value to truncate the body to 10240 bytes. */
    int TRUNCATION_BYTES_10240 = 5;
    /** The ActiveSync Truncation value to truncate the body to 20480 bytes. */
    int TRUNCATION_BYTES_20480 = 6;
    /** The ActiveSync Truncation value to truncate the body to 51200 bytes. */
    int TRUNCATION_BYTES_51200 = 7;
    /** The ActiveSync Truncation value to truncate the body to 102400 bytes. */
    int TRUNCATION_BYTES_102400 = 8;
    /** The ActiveSync Truncation value to not truncate the body. */
    int TRUNCATION_BYTES_ALL = 9;

    /** The ActiveSync MIMETruncation value to truncate the body to 0 bytes. */
    int MIME_TRUNCATION_BYTES_0 = 0;
    /** The ActiveSync MIMETruncation value to truncate the body to 512 bytes. */
    int MIME_TRUNCATION_BYTES_4096 = 1;
    /** The ActiveSync MIMETruncation value to truncate the body to 1024 bytes. */
    int MIME_TRUNCATION_BYTES_5120 = 2;
    /** The ActiveSync MIMETruncation value to truncate the body to 2048 bytes. */
    int MIME_TRUNCATION_BYTES_7168 = 3;
    /** The ActiveSync MIMETruncation value to truncate the body to 5120 bytes. */
    int MIME_TRUNCATION_BYTES_10240 = 4;
    /** The ActiveSync MIMETruncation value to truncate the body to 10240 bytes. */
    int MIME_TRUNCATION_BYTES_20480 = 5;
    /** The ActiveSync MIMETruncation value to truncate the body to 20480 bytes. */
    int MIME_TRUNCATION_BYTES_51200 = 6;
    /** The ActiveSync MIMETruncation value to truncate the body to 51200 bytes. */
    int MIME_TRUNCATION_BYTES_102400 = 7;
    /** The ActiveSync MIMETruncation value to not truncate the body. */
    int MIME_TRUNCATION_BYTES_ALL = 8;

    /** The MeetingRequest BusyStatus element value for Busy. */
    int EMAIL_BUSY_STATUS_BUSY = 0;
    /** The MeetingRequest BusyStatus element value for Free. */
    int EMAIL_BUSY_STATUS_FREE = 1;
    /** The MeetingRequest BusyStatus element value for Tentative. */
    int EMAIL_BUSY_STATUS_TENTATIVE = 2;
    /** The MeetingRequest BusyStatus element value for Out of Office. */
    int EMAIL_BUSY_STATUS_OUT_OF_OFFICE = 3;

    /** The Calendar Attendee/Status element value for unknown status. */
    int CAL_ATTENDEE_STATUS_UNKNOWN = 0;
    /** The Calendar Attendee/Status element value for tentatively accepted. */
    int CAL_ATTENDEE_STATUS_TENTATIVE = 2;
    /** The Calendar Attendee/Status element value for accepted. */
    int CAL_ATTENDEE_STATUS_ACCEPT = 3;
    /** The Calendar Attendee/Status element value for declined. */
    int CAL_ATTENDEE_STATUS_DECLINE = 4;
    /** The Calendar Attendee/Status element value for no response yet. */
    int CAL_ATTENDEE_STATUS_NOT_RESPONDED = 5;

    /** The Calendar Attendee/Type element value for required attendee. */
    int CAL_ATTENDEE_TYPE_REQUIRED = 1;
    /** The Calendar Attendee/Type element value for optional attendee. */
    int CAL_ATTENDEE_TYPE_OPTIONAL = 2;
    /** The Calendar Attendee/Type element value for resource, e.g. meeting room. */
    int CAL_ATTENDEE_TYPE_RESOURCE = 3;

    /** The Calendar BusyStatus element value for Free. */
    int CAL_BUSY_STATUS_FREE = 0;
    /** The Calendar BusyStatus element value for Tentative. */
    int CAL_BUSY_STATUS_TENTATIVE = 1;
    /** The Calendar BusyStatus element value for Busy. */
    int CAL_BUSY_STATUS_BUSY = 2;
    /** The Calendar BusyStatus element value for Out of Office. */
    int CAL_BUSY_STATUS_OUT_OF_OFFICE = 3;

    /** The Calendar MeetingStatus element value for Not a Meeting. */
    int CAL_MEETING_STATUS_NOT_A_MEETING = 0;
    /** The Calendar MeetingStatus element value for Is a Meeting. */
    int CAL_MEETING_STATUS_MEETING = 1;
    /** The Calendar MeetingStatus element value for Meeting is Received. */
    int CAL_MEETING_STATUS_MEETING_RECEIVED = 3;
    /** The Calendar MeetingStatus element value for Meeting is Canceled. */
    int CAL_MEETING_STATUS_MEETING_CANCELED = 5;

    /** The Calendar Sensitivity element value for Normal. */
    int CAL_SENSITIVITY_NORMAL = 0;
    /** The Calendar Sensitivity element value for Personal. */
    int CAL_SENSITIVITY_PERSONAL = 1;
    /** The Calendar Sensitivity element value for Private. */
    int CAL_SENSITIVITY_PRIVATE = 2;
    /** The Calendar Sensitivity element value for Confidential. */
    int CAL_SENSITIVITY_CONFIDENTIAL = 3;

    /** The Task Importance element value for Low. */
    int TASK_IMPORTANCE_LOW = 0;
    /** The Task Importance element value for Normal. */
    int TASK_IMPORTANCE_NORMAL = 1;
    /** The Task Importance element value for High. */
    int TASK_IMPORTANCE_HIGH = 2;

    /** The Task Sensitivity element value for normal */
    int TASK_SENSITIVITY_NORMAL = 0;
    /** The Task Sensitivity element value for personal */
    int TASK_SENSITIVITY_PERSONAL = 1;
    /** The Task Sensitivity element value for private */
    int TASK_SENSITIVITY_PRIVATE = 2;
    /** The Task Sensitivity element value for confidential */
    int TASK_SENSITIVITY_CONFIDENTIAL = 3;

    /** The last part of the EM_MessageClass value for a normal email message. */
    String MESSAGE_CLASS_SUFFIX_MESSAGE = "Note";
    /** The last part of the EM_MessageClass value for an out of office response. */
    String MESSAGE_CLASS_SUFFIX_OUT_OF_OFFICE = "Microsoft";
    /** The last part of the EM_MessageClass value for a secure MIME message. */
    String MESSAGE_CLASS_SUFFIX_SMIME = "SMIME";
    /** The last part of the EM_MessageClass value for a signed S/MIME message. */
    String MESSAGE_CLASS_SUFFIX_SMIME_SIGNED = "MultipartSigned";
    /** The last part of the EM_MessageClass value for a meeting request. */
    String MESSAGE_CLASS_SUFFIX_MEETING_REQUEST = "Request";
    /** The last part of the EM_MessageClass value for a meeting cancellation. */
    String MESSAGE_CLASS_SUFFIX_MEETING_CANCELED = "Canceled";
    /** The last part of the EM_MessageClass value for a meeting acceptance. */
    String MESSAGE_CLASS_SUFFIX_MEETING_ACCEPTED = "Pos";
    /** The last part of the EM_MessageClass value for a meeting tentatively accepted. */
    String MESSAGE_CLASS_SUFFIX_MEETING_TENTATIVE = "Tent";
    /** The last part of the EM_MessageClass value for a meeting declined. */
    String MESSAGE_CLASS_SUFFIX_MEETING_DECLINED = "Neg";
    /** The last part of the EM_MessageClass value for a Post. */
    String MESSAGE_CLASS_SUFFIX_POST = "Post";

    /** This isn't a meeting response (used when generating emails). */
    int MEETING_RESPONSE_NONE = -1;
    /** The MeetingResponse command's UserResponse value for "accept" */
    int MEETING_RESPONSE_ACCEPT = 1;
    /** The MeetingResponse command's UserResponse value for "tentatively accept" */
    int MEETING_RESPONSE_TENTATIVE = 2;
    /** The MeetingResponse command's UserResponse value for "reject" */
    int MEETING_RESPONSE_REJECT = 3;
    int MEETING_RESPONSE_MAX = MEETING_RESPONSE_REJECT;

    /** The MIMESupport element value to never sent MIME data (the default). */
    String MIME_SUPPORT_NEVER = "0";
    /** The MIMESupport element value to send MIME data only for SMIME messages. */
    String MIME_SUPPORT_SMIME_ONLY = "1";
    /** The MIMESupport element value to send MIME data always. */
    String MIME_SUPPORT_ALWAYS = "2";

    /** Broadcast when the ActiveSync database has been reinitialized. */
    String ACTION_DB_RESET = "com.motorola.blur.service.email.protocol.activesync.Action.DB_RESET";

    /** Broadcast when the ActiveSync folder hierarchy has been reset because of a sync error. */
    String ACTION_EAS_FOLDERS_RESYNC = "com.motorola.blur.service.email.protocol.activesync.Action.FOLDERS_RESYNC";
    /** The Intent Extra key for the Account ID. Used by various actions. */
    String EXTRA_ACCOUNT_ID = "account_id";

    /** The Intent Extra key for the old protocol version number. */
    String EXTRA_OLD_VERSION = "old_version";
    /** The Intent Extra key for the new protocol version number. */
    String EXTRA_NEW_VERSION = "new_version";

    /** This intent is used to start the policy setting service when ActiveSync detects a policy change from the server. */
    String ACTION_EAS_SET_POLICY = "com.motorola.blur.service.email.protocol.activesync.Action.SET_POLICY";

    /** The Intent Extra key for the boolean value to indicate whether the policy admin should be re-activated. */
    String EXTRA_REACTIVATE_ADMIN = "reactivate_admin";

    /** This intent is used to request a remote wipe  */
    String ACTION_EAS_REMOTE_WIPE = "com.motorola.blur.service.email.protocol.activesync.Action.REMOTE_WIPE";

    /** This intent is used to request the email service to send a recovery password to the Exchange server. */
    String ACTION_EAS_SEND_RECOVERY_PWD = "com.motorola.blur.service.email.protocol.activesync.Action.SEND_RECOVERY_PWD";

    /** This should be sent as the startService() action once a folder has been synced. It's used by
     * the push service to detected when all required folders have been synced so that it can send the next
     * Ping command. The intent should define the extras "account_id" (a long) and "folder_id" (as string).
     */
    String ACTION_EAS_FOLDER_SYNCED = "com.motorola.blur.service.email.protocol.activesync.Action.FOLDER_SYNCED";

    /** The Intent Extra key for the Folder (remote) ID. Used by ACTION_EAS_FOLDER_SYNCED. */
    String EXTRA_FOLDER_ID = "folder_id";

    /** The Intent Extra key for the count of Add commands in the last sync. Used by ACTION_EAS_FOLDER_SYNCED. */
    String EXTRA_ADD_COUNT = "add_count";

    /** The Intent Extra key for the count of Delete commands in the last sync. Used by ACTION_EAS_FOLDER_SYNCED. */
    String EXTRA_DELETE_COUNT = "delete_count";

    /** The Intent Extra key for the count of Change commands in the last sync. Used by ACTION_EAS_FOLDER_SYNCED. */
    String EXTRA_CHANGE_COUNT = "change_count";

    /** The permission required to receive ACTION_FOLDERS_RESYNC. */
    // Reuse the email engine permission.
    String PERMISSION_FOLDERS_RESYNC = "com.motorola.blur.service.email.Permissions.EMAIL_ENGINE";

    /** The permission required to receive ACTION_EAS_SEND_RECOVERY_PWD. */
    String PERMISSION_SEND_RECOVERY_PWD = PERMISSION_FOLDERS_RESYNC;

    /** The 12.x Body element Type for Plain Text */
    int BODY_TYPE_PLAIN_TEXT = 1;
    /** The 12.x Body element Type for HTML */
    int BODY_TYPE_HTML = 2;
    /** The 12.x Body element Type for RTF */
    int BODY_TYPE_RTF = 3;
    /** The 12.x Body element Type for MIME data */
    int BODY_TYPE_MIME = 4;

    /** ActiveSync Out of Office State */
    int OOF_DISABLED = 0;
    int OOF_GLOBAL = 1;
    int OOF_TIMEBASED = 2;

    /** OOF auto reply message type */
    String OOF_BODY_TYPE_HTML = "HTML";
    String OOF_BODY_TYPE_TEXT = "TEXT";

    /**
     * The Policy Key value send when the device hasn't been provisioned for the account.
     * or doesn't need to be.
     */
    String POLICY_KEY_NOT_PROVISIONED = "0";

    /**
     * The Policy Key stored in the ActiveSync DB to mean that the account doesn't need provisioning,
     * so we won't attempt to pre-emptively provision before each command.
     */
    String POLICY_KEY_NOT_REQUIRED = "-1";

    /**
     * The Policy Key stored in the ActiveSync DB to mean that the device isn't provisionable
     * according to the account's policies. In this state we abandon any command before
     * we even get started. It's reset whenever the user attempts a manual email sync.
     */
    String POLICY_KEY_NOT_PROVISIONABLE = "-2";

    /**
     * The EAS 2.5 PasswordComplexity provisioning value for "require alphanumeric"
     */
    int EAS25_PROV_PWD_COMPLEXITY_ALPHANUM = 0;
    /**
     * The EAS 2.5 PasswordComplexity provisioning value for "require numeric" (not used)
     */
    int EAS25_PROV_PWD_COMPLEXITY_NUM = 1;
    /**
     * The EAS 2.5 PasswordComplexity provisioning value for "require alpha OR numeric"
     */
    int EAS25_PROV_PWD_COMPLEXITY_ALPHA_OR_NUM = 2;

    /** The AllowBluetooth provisioning key value to never allow Bluetooth. */
    int PV_ALLOW_BLUETOOTH_NEVER = 0;
    /** The AllowBluetooth provisioning key value to allow Bluetooth for hands-free operation. */
    int PV_ALLOW_BLUETOOTH_HANDSFREE = 1;
    /** The AllowBluetooth provisioning key value to allow Bluetooth in any circumstances. */
    int PV_ALLOW_BLUETOOTH_ALWAYS = 2;

    /** The AllowSMIMENegotiation provisioning key value to never allow algorithm negotiation. */
    int PV_ALLOW_SMIME_NEGOTIATION_NEVER = 0;
    /** The AllowSMIMENegotiation provisioning key value to allow strong algorithm negotiation. */
    int PV_ALLOW_SMIME_NEGOTIATION_STRONG = 1;
    /** The AllowSMIMENegotiation provisioning key value to allow any algorithm negotiation. */
    int PV_ALLOW_SMIME_NEGOTIATION_ANY = 2;

    /** The value for email body truncation policies that means "no truncation". */
    int PV_EMAIL_TRUNCATION_NONE = -1;
    /** The value for email body truncation policies that means "truncate to headers only". */
    int PV_EMAIL_TRUNCATION_HEADERS_ONLY = 0;

    /** The value for provisioning policy that means "no limit". */
    String PV_NO_LIMIT = "-1";

    /* The highest protocol version number we support. */
    String HIGHEST_SUPPORTED_EAS_VERSION = "12.1";

    /** The protocol version number we use if we can't parse the value from the server.*/
    String FALLBACK_EAS_VERSION = "2.5";

    /** Policy for how to handle deleted folders in email. */
    boolean HIDE_DELETED_FOLDERS_EMAIL = false;
    /** Policy for how to handle deleted folders in contacts. */
    boolean HIDE_DELETED_FOLDERS_CONTACTS = true;
    /** Policy for how to handle deleted folders in calendar. */
    boolean HIDE_DELETED_FOLDERS_CALENDAR = true;
    /** Policy for how to handle deleted folders in tasks. */
    boolean HIDE_DELETED_FOLDERS_TASKS = true;

}
