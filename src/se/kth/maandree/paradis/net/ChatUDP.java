/**
 *  Paradis — Ever growing network for parallell and distributed computing.
 *  Copyright © 2012  Mattias Andrée
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.kth.maandree.paradis.net;
import se.kth.maandree.paradis.io.*;

import java.io.*;
import java.net.*;


/**
 * Test UDP peer
 * 
 * @author  Mattias Andrée, <a href="mailto:maandree@kth.se">maandree@kth.se</a>
 */
public class ChatUDP implements Runnable
{
    public ChatUDP(final int port) throws IOException
    {
	socket = new DatagramSocket(port);
	this.port = socket.getLocalPort();
	
	(new Thread(this)).start();
    }
    
    
    private final DatagramSocket socket;
    
    public final int port;
    
    protected boolean closing = false;
    
    
    public synchronized void send(final String host, final int port, final String data) throws IOException
    {
	final InetAddress address = InetAddress.getByName(host);
	final byte[] bytes = data.getBytes("UTF-8");
	
	final DatagramPacket packet = new DatagramPacket(bytes, 0, bytes.length, address, port);
	
	this.socket.send(packet);
    }
    
    public void close() throws IOException
    {
	this.closing = true;
	this.socket.close();
    }
    
    public void run()
    {
	try
	{
	    final byte[] bytes = new byte[0x8000];
	    final DatagramPacket packet = new DatagramPacket(bytes, 0, bytes.length);
	    
	    for (;;)
	    {
		this.socket.receive(packet);
		
		final String msg = new String(packet.getData(), packet.getOffset(), packet.getLength(), "UTF-8");
		System.out.println("\033[34m[" + packet.getAddress().getHostAddress() + "]:" + packet.getPort() + ":\033[39m " + msg);
	    }
	}
	catch (final Throwable err)
	{
	    if (this.closing == false)
		err.printStackTrace(System.err);
	}
    }
    
}

