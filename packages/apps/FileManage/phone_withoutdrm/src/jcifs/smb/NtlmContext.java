/* jcifs smb client library in Java
 * Copyright (C) 2008  "Michael B. Allen" <jcifs at samba dot org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jcifs.smb;

import java.io.IOException;
import java.security.*;
import jcifs.ntlmssp.*;

/**
 * For initiating NTLM authentication (including NTLMv2). If you want to add
 * NTLMv2 authentication support to something this is what you want to use. See
 * the code for details. Note that JCIFS does not implement the acceptor side of
 * NTLM authentication.
 */

public class NtlmContext {

    NtlmPasswordAuthentication auth;
    int ntlmsspFlags;
    String workstation;
    boolean isEstablished = false;
    byte[] serverChallenge = null;
    byte[] signingKey = null;
    int state = 1;

    public NtlmContext(NtlmPasswordAuthentication auth, boolean doSigning) {
	this.auth = auth;
	this.ntlmsspFlags = ntlmsspFlags | NtlmFlags.NTLMSSP_REQUEST_TARGET
		| NtlmFlags.NTLMSSP_NEGOTIATE_NTLM2
		| NtlmFlags.NTLMSSP_NEGOTIATE_128;
	if (doSigning) {
	    this.ntlmsspFlags |= NtlmFlags.NTLMSSP_NEGOTIATE_SIGN
		    | NtlmFlags.NTLMSSP_NEGOTIATE_ALWAYS_SIGN
		    | NtlmFlags.NTLMSSP_NEGOTIATE_KEY_EXCH;
	}
	this.workstation = Type1Message.getDefaultWorkstation();
    }

    public boolean isEstablished() {
	return isEstablished;
    }

    public byte[] getServerChallenge() {
	return serverChallenge;
    }

    public byte[] getSigningKey() {
	return signingKey;
    }

    public byte[] initSecContext(byte[] token, int offset, int len)
	    throws SmbException {
	switch (state) {
	case 1:
	    Type1Message msg1 = new Type1Message(ntlmsspFlags,
		    auth.getDomain(), workstation);
	    token = msg1.toByteArray();
	    state++;
	    break;
	case 2:
	    try {
		Type2Message msg2 = new Type2Message(token);

		serverChallenge = msg2.getChallenge();
		ntlmsspFlags &= msg2.getFlags();

		Type3Message msg3 = new Type3Message(msg2, auth.getPassword(),
			auth.getDomain(), auth.getUsername(), workstation,
			ntlmsspFlags);
		token = msg3.toByteArray();

		if ((ntlmsspFlags & NtlmFlags.NTLMSSP_NEGOTIATE_SIGN) != 0)
		    signingKey = msg3.getMasterKey();

		isEstablished = true;
		state++;
		break;
	    } catch (Exception e) {
		throw new SmbException(e.getMessage(), e);
	    }
	default:
	    throw new SmbException("Invalid state");
	}
	return token;
    }
}
