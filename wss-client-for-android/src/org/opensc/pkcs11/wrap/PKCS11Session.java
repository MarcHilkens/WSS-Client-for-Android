/***********************************************************
 * $Id: PKCS11Session.java 25 2006-11-30 08:17:07Z wolfgang.glas $
 * 
 * PKCS11 provider of the OpenSC project http://www.opensc-project.org
 *
 * Copyright (C) 2002-2006 ev-i Informationstechnologie GmbH
 *
 * Created: Jul 19, 2006
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 * 
 ***********************************************************/

package org.opensc.pkcs11.wrap;

import javax.security.auth.DestroyFailedException;

import org.opensc.util.DestroyableHolder;
import org.opensc.util.Util;

/**
 * @author wglas
 *
 * This class represents an open session on a token.
 */
public class PKCS11Session extends DestroyableHolder
{
	/**
	 * The C handle of the provider.
	 */
	protected long pvh;
	
	/**
	 * The C handle of the slot.
	 */
	protected long shandle;

	/**
	 * The C handle of the session.
	 */
	protected long handle;

	private boolean userLoggedIn;
	
	private boolean SOLoggedIn;
	
	public static final int OPEN_MODE_READ_ONLY = 0;
	public static final int OPEN_MODE_READ_WRITE = 1;

	/**
	 * The counterpart of SKU_SO in pkcs11t.h, used to present the
	 * security officer PIN to the card.
	 */
	private static int LOGIN_TYPE_SO = 0;
	/**
	 * The counterpart of SKU_USER in pkcs11t.h, used to present the
	 * user PIN to the card.
	 */
	private static int LOGIN_TYPE_USER = 1;

	/**
	 * Contruct a session from a given handle-
	 */
	protected PKCS11Session(PKCS11Slot slot, long handle)
	{
		super(slot);
		this.pvh = slot.getPvh();
		this.shandle = slot.getHandle();
		this.handle = handle;
		this.userLoggedIn = false;
		this.SOLoggedIn = false;
	}

	private static native long openNative(long pvh, long shandle, int mode) throws PKCS11Exception;
	private static native void closeNative(long pvh, long shandle, long handle);
	
	/**
	 * Opens a session on the given slot.
	 * 
	 * @param slot The slot on which we open the session.
	 * @param mode Either OPEN_MODE_READ_ONLY or OPEN_MODE_READ_WRITE
	 * @return The open session.
	 * @throws PKCS11Exception Upon errors of the underlying PKCS#11 module.
	 */
	public static PKCS11Session open(PKCS11Slot slot, int mode) throws PKCS11Exception
	{
		long handle = openNative(slot.getPvh(),slot.getHandle(),mode);
		return new PKCS11Session(slot,handle);
	}

	private native void loginNative(long _pvh, long _shandle, long _handle, int type, byte[] pin) throws PKCS11Exception;
	
	/**
	 * Presents the user PIN to the token. Should only be called after open().
	 * 
	 * @param pin The user pin. This paremeter may be <code>null</code>, if the
	 *            token has a protected authentication path.
	 *            
	 * @throws PKCS11Exception Upon errors of the underlying PKCS#11 engine.
	 *            
	 * @see PKCS11Slot#hasTokenProtectedAuthPath()
	 */
	public void loginUser(char[] pin) throws PKCS11Exception
	{
		if (this.userLoggedIn)
			throw new PKCS11Exception("The user is already logged in.");

		loginNative(this.pvh,this.shandle,this.handle,LOGIN_TYPE_USER,Util.translatePin(pin));
		
        this.userLoggedIn = true;
	}
	
	/**
	 * Presents the security officer PIN to the token. Should only be called after open().
	 * 
	 * @param pin The SO pin. This paremeter may be <code>null</code>, if the
	 *            token has a protected authentication path.
	 *            
	 * @throws PKCS11Exception Upon errors of the underlying PKCS#11 engine.
	 *            
	 * @see PKCS11Slot#hasTokenProtectedAuthPath()
	 */
	public void loginSO(char[] pin) throws PKCS11Exception
	{
		if (this.SOLoggedIn)
			throw new PKCS11Exception("The security officer is already logged in.");
		
		loginNative(this.pvh,this.shandle,this.handle,LOGIN_TYPE_SO,Util.translatePin(pin));
		
        this.SOLoggedIn = true;
	}

	/**
	 * @return Returns, whether the security officer has successfully logged in
	 *         through loginSO().
	 */
	public boolean isSOLoggedIn()
	{
		return this.SOLoggedIn;
	}

	/**
	 * @return Returns, whether the user has successfully logged in
	 *         through loginUser().
	 */
	public boolean isUserLoggedIn()
	{
		return this.userLoggedIn;
	}
	
	private native void logoutNative(long _pvh, long _shandle, long _handle) throws PKCS11Exception;
	
	/**
	 * Logs out from the token.
	 */
	public void logout() throws PKCS11Exception
	{
		if (!this.userLoggedIn && ! this.SOLoggedIn) return;
		
		logoutNative(this.pvh,this.shandle,this.handle);
		
        this.userLoggedIn = false;
        this.SOLoggedIn = false;
	}

	/* (non-Javadoc)
	 * @see org.opensc.util.DestroyableChild#destroy()
	 */
	@Override
	public void destroy() throws DestroyFailedException
	{
		closeNative(this.pvh,this.shandle,this.handle);
        this.handle = 0;
        this.shandle = 0;
        this.pvh = 0;
        this.userLoggedIn = false;
        this.SOLoggedIn = false;
		super.destroy();
	}

	/**
	 * @return Returns the C handle of the underlying provider.
	 */
	protected long getPvh()
	{
		return this.pvh;
	}
	
	/**
	 * @return Returns the C handle of the slot.
	 */
	protected long getSlotHandle()
	{
		return this.shandle;
	}
	
	/**
	 * @return Returns the C handle of the session.
	 */
	protected long getHandle()
	{
		return this.handle;
	}
}
