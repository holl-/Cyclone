/*
 *	EsdStream.java
 */

/*
 *  Copyright (c) 1999 by Matthias Pfisterer <Matthias.Pfisterer@gmx.de>
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */


package	org.tritonus.lowlevel.esd;


import	java.io.IOException;


public class EsdStream
{
	static
	{
		System.loadLibrary("esdtritonus");
	}



	/**	Holds socket fd to EsounD.
	 *	This field is long because on 64 bit architectures, the native
	 *	size of ints may be 64 bit.
	 */
	private long			m_lNativeFd;



	/**	Holds player id of the stream in EsounD.
	 *	This field is long because on 64 bit architectures, the native
	 *	size of ints may be 64 bit.
	 */
	private long			m_lNativePlayerId;





	public EsdStream()
	{
	}



	/**	Opens the connection to esd and initiates a stream.
	 *
	 */	
	public native void open(int nFormat, int nSampleRate);



	/**	Writes a block of data to esd.
	 *	Before using this method, you have to open a connection
	 *	to esd with open(). After being done, call close() to
	 *	release native and server-side resources.
	 *
	 *	@return	the number of bytes written
	 */
	public native int write(byte[] abData, int nOffset, int nLength);



	/**	Closes the connection to esd.
	 *	With this call, all resources inside esd associated with
	 *	this stream are freed.???
	 *	Calls to the write() method are no longer allowed after
	 *	return from this call.
	 */
	public native void close();



	/**	Sets the volume for this stream.
	 *	The values for the volume should be normalized
	 *	to 256 (256 means 0 dB).
	 */
	public native void setVolume(int nLeft, int nRight);

}



/*** EsdStream.java ***/
