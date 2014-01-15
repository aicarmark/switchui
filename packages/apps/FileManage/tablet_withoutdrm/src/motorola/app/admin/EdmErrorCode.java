/*
 * Copyright (C) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR)
 */


package motorola.app.admin;


import android.content.Intent;
import android.content.Context;
import android.os.Bundle;

/**
* @hide
* Error exception class mapping EDM error codes
* @author MIEL EDM Team
*/
public final class EdmErrorCode
{
    public static final String ACTION_EDM_ERROR_STATUS = "mot.app.admin.edm.ERROR_STATUS";
    public static final String ERROR_CODE = "error_code";

    private Context mContext;

    /**
    * @hide
    * Creates a EdmErrorCode object with the specified context.
    */
    public EdmErrorCode(Context c) {
        mContext = c;
    }

    /**
    * @hide
    * No Error, No warning - Genral Success.
    */
    public static final int    EDM_ERROR_STATUS_SUCCESS                           = 0;


    /**
    * @hide
    *  Error codes related to Pattern Delete
    */


    /**
    * @hide
    * Not able to delete the file of specific pattern.
    */
    public static final int    EDM_ERROR_STATUS_PATTERN_FAILED_TO_DELETE          = -2000;

    /**
    * @hide
    * Specific pattern not found on the device.
    */
    public static final int    EDM_ERROR_STATUS_PATTERN_NOT_FOUND_TO_DELETE       = -2001;

    /**
    * @hide
    * Specific pattern not found on the device.
    */
    public static final int    EDM_ERROR_STATUS_SD_CARD_NOT_FOUND                 = -2002;

    /**
    * @hide
    * Error codes related to VPN configuration.
    */


    /**
    * @hide
    * Invalid L2TP Seceret
    */
    public static final int    EDM_ERROR_STATUS_VPN_INVALID_L2TP_SECRET          = -3001;

    /**
    * @hide
    * Invalid CA Certificate
    */
    public static final int    EDM_ERROR_STATUS_VPN_INVALID_CA_CERTIFICATE        = -3002;

    /**
    * @hide
    * Invalid USER Certificate
    */
    public static final int    EDM_ERROR_STATUS_VPN_INVALID_USER_CERTIFICATE      = -3003;

    /**
    * @hide
    * Invalid IPSec PRE-SHARED KEY
    */
    public static final int    EDM_ERROR_STATUS_VPN_INVALID_IPSEC_PRE_SHARED_KEY  = -3004;

    /**
    * @hide
    * Invalid VPN TYPE
    */
    public static final int    EDM_ERROR_STATUS_VPN_INVALID_VPN_TYPE              = -3005;

    /**
    * @hide
    * VPN profile already exits for specific name.
    */
    public static final int    EDM_ERROR_STATUS_VPN_NAME_ALREADY_EXISTS           = -3006;

    /**
    * @hide
    * Invalid VPN ID
    */
    public static final int    EDM_ERROR_STATUS_VPN_INVALID_ID                    = -3007;

    /**
    * @hide
    * Invalid VPN Name OR VPN Server name.
    */
    public static final int    EDM_ERROR_STATUS_VPN_INVALID_SERVER_NAME           = -3008;

    /**
    * @hide
    * VPN add failed
    */
    public static final int    EDM_ERROR_STATUS_VPN_ADD_FAILED                    = -3009;

    /**
    * @hide
    * VPN delete failed due to invaild vpn ID
    */
    public static final int    EDM_ERROR_STATUS_VPN_INVALID_ID_DELETE_FAILED      = -3010;

    /**
    * @hide
    * VPN fetch failed due to invaild vpn ID
    */
    public static final int    EDM_ERROR_STATUS_VPN_INVALID_ID_FETCH_FAILED       = -3011;


    /**
    * @hide
    *  Error codes related to Eas Configuration
    */


    /**
    * @hide
    * Eas account Already exist
    */
    public static final int    EDM_ERROR_STATUS_EAS_ACCT_ALREADY_EXISTS           = -4000;

    /**
    * @hide
    * Eas account add failed
    */
    public static final int    EDM_ERROR_STATUS_EAS_ACCT_ADD_FAILED               = -4001;

    /**
    * @hide
    * Eas fetch failed as account does not exist
    */
    public static final int    EDM_ERROR_STATUS_EAS_ACCT_FETCH_FAILED             = -4002;

    /**
    * @hide
    * Eas account delete failed as account does not exist
    */
    public static final int    EDM_ERROR_STATUS_EAS_ACCT_DELETE_FAILED            = -4003;

    /**
    * @hide
    * Invalid agruments are passed to the installCertificate() method
    */
    public static final int EDM_ERROR_STATUS_CERT_INVALID_ARGUMENTS               = -5001;

    /**
    * @hide
    *  Installation of Certtificate is cancelled
    */
    public static final int EDM_ERROR_STATUS_CERT_CANCELLED                       = -5002;

    /**
    * @hide
    *  Not a valid password for the given data
    */
    //IKDROIDPRO-503
    public static final int EDM_ERROR_STATUS_CERT_WRONG_PASSWORD_OR_INVALID_DATA  = -5003;

    /**
    * @hide
    *  Invalid charecters in the Certificate name
    */
    public static final int EDM_ERROR_STATUS_CERT_INVALID_CERTNAME                = -5004;

    /**
    * @hide
    *  Empty certificate
    */
    //IKDROIDPRO-503
    public static final int EDM_ERROR_STATUS_CERT_EMPTY_CERTDATA                  = -5005;

    /**
    * @hide
    * This type of certificate is not mentioned valid
    */
    public static final int EDM_ERROR_STATUS_CERT_INVALID_CERTTYPE                = -5006;

    /**
    * @hide
    * Unable to save the certificate.
    */
    public static final int EDM_ERROR_STATUS_CERT_UNABLE_TO_SAVE_CERTIFICATE      = -5007;

    /**
    * @hide
    * Certificate is too large
    */
    public static final int EDM_ERROR_STATUS_CERT_TOO_LARGE_CERTIFICATE           = -5008;
    /**
    * @hide
    * Deletion of Certificate is failed
    */
    //IKDROIDPRO-503
    public static final int EDM_ERROR_STATUS_CERT_DELETION_FAILED                 = -5009;

    /**
     * @hide
     * Refer the individual API for identifiying extraArg
     */
    public void returnEdmStatus(int edmStatusErrorCode, Bundle extraArg) {
        Intent intent = new Intent(ACTION_EDM_ERROR_STATUS);
        intent.putExtra(ERROR_CODE, edmStatusErrorCode);
        intent.putExtra("EXTRA_ARGS", extraArg);
        mContext.sendBroadcast(intent);
    }
}


