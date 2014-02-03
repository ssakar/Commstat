/*
	Commstat - Funf-based Sensor Application 
	Copyright (C) 2013 Serkan Sakar

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


package de.tu_berlin.snet.mail;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * MyTrustManager - NOT SECURE
 */
public class MyTrustManager implements X509TrustManager {

	public void checkClientTrusted(X509Certificate[] cert, String authType) {
		// everything is trusted
	}

	public void checkServerTrusted(X509Certificate[] cert, String authType) {
		// everything is trusted
	}

	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}
}
