package openfire.chat.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import openfire.chat.activity.LoginActivity;
import openfire.chat.activity.RegisterActivity;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Registration;

import android.text.TextUtils;
import android.util.Log;

public class UserServiceImpl implements UserService {

	/**your openfire (social interactions) server port */
	public static final int SERVER_PORT = 5220; //server port
	/**your openfire (socila interactions) server IP address */
	public static String SERVER_HOST = "129.128.184.46";// server ip
	/**your server name*/
	public static String SERVER_NAME = "myria";// server name
	/** XMPP connection: to build connection*/
	private XMPPConnection connection = null;
	
	/**user login function. You need to call {@link XMPPConnection#login(String, String)}.
	 * 
	 * @param username login user name
	 * @param password login password
	 * @return connection XMPP connection
	 * @throws Exception throw a {@link ServiceException} which extends Exception
	 * */
	@Override
	public XMPPConnection userLogin(String username, String password)
			throws Exception {

//		try {
//			if (null == connection || !connection.isAuthenticated()) {
//				XMPPConnection.DEBUG_ENABLED = true;
//
//				ConnectionConfiguration config = new ConnectionConfiguration(
//						SERVER_HOST, SERVER_PORT, SERVER_NAME);
//				config.setReconnectionAllowed(true);
//				config.setSendPresence(true);
//				config.setSASLAuthenticationEnabled(true);
//				connection = new XMPPConnection(config);
//				if(!connection.isConnected()){
//					connection.connect();
//					Log.i("connection.getServiceName();", connection.getServiceName());
//					connection.login(username, password);
//				}
//				return connection;
//			}
//		} catch (XMPPException xe) {
//			Log.e("GET CONNECTION", xe.toString());
//			throw new ServiceException(LoginActivity.SERVER_ERROR);
//
//		}
		
		if(connection == null || !connection.isAuthenticated())
			connection = GetConnection();

		try {
			connection.login(username, password);
		} catch (XMPPException xe) {
			Log.e("LOGIN", "Failed to log in as " + username);
			Log.e("LOGIN", xe.toString());
			throw new ServiceException(LoginActivity.UNKNOW_ERROR);
		}
		return connection;
	}

	/** user register method, you should call {@link XMPPConnection#createPacketCollector(PacketFilter)} method.
	 * 
	 * @param username user name should be unique, not null
	 * @param name display name (or nick name)
	 * @param email user's email
	 * @param password password
	 * @param confirmPassword confirm password
	 * @return connection Return an XMPP connection if registered successfully, or null if not.
	 * @throws Exception throw a {@link ServiceException} which extends Exception
	 * */
	public XMPPConnection userRegister(String username, String name, String email, String password,
			String confirmPassword) throws Exception {

		if (TextUtils.isEmpty(username)) {// username == null ||
											// username.equals("")
			throw new ServiceException(
					RegisterActivity.REGISTER_USERNAME_FAILED);
		}
		if (TextUtils.isEmpty(email)) {
			throw new ServiceException(RegisterActivity.REGISTER_EMAIL_FAILED);
		}
		if (password.length() < 6 || password.length() > 20) {
			throw new ServiceException(
					RegisterActivity.REGISTER_PASSWORD_LENGTH);
		}
		if (!password.equals(confirmPassword)) {
			throw new ServiceException(RegisterActivity.REGISTER_PASSWORD_DIFF);
		}
		if (!(validteUsername(username))) {
			throw new ServiceException(RegisterActivity.USERNAME_NOT_VALIDATE);
		}
		if (!(validteEmail(email))) {
			throw new ServiceException(RegisterActivity.EMAIL_NOT_VALIDATE);
		}

		Registration reg = new Registration();
		reg.setType(IQ.Type.SET);

		if(connection == null )
			connection = GetConnection();
		
		reg.setTo(connection.getServiceName());
		reg.setUsername(username);
		reg.setPassword(password);
		reg.addAttribute("name", name);
		reg.addAttribute("email", email);

		reg.addAttribute("android", "geolo_createUser_android");
		PacketFilter filter = new AndFilter(new PacketIDFilter(
				reg.getPacketID()), new PacketTypeFilter(IQ.class));
		PacketCollector collector = connection.createPacketCollector(filter);
		connection.sendPacket(reg);
		IQ result = (IQ) collector.nextResult(SmackConfiguration
				.getPacketReplyTimeout());
		// Stop querying and to get results
		collector.cancel();

		if (result == null) {
			throw new ServiceException(RegisterActivity.UNKNOW_ERROR);
		} else if (result.getType() == IQ.Type.ERROR) {
			if (result.getError().toString().equalsIgnoreCase("conflict(409)")) {
				throw new ServiceException(
						RegisterActivity.USERNAME_EXISTED);
			} else {
				throw new ServiceException(RegisterActivity.REGISTER_FAILED);
			}
		} else if (result.getType() == IQ.Type.RESULT) {
			return connection;
		}

		return null;
	}

	/**Get XMPP connection function. It is used to connect the conection between clients and server
	 *  You need to call {@link XMPPConnection#connect()}.
	 *  
	 * @return connection XMPP connection
	 * @throws ServiceException Exception which extends Exception
	 * */
	public XMPPConnection GetConnection() throws ServiceException{

		XMPPConnection.DEBUG_ENABLED = true;

		ConnectionConfiguration config = new ConnectionConfiguration(
				SERVER_HOST, SERVER_PORT, SERVER_NAME);
		config.setReconnectionAllowed(true);
		config.setSendPresence(true);
		config.setSASLAuthenticationEnabled(true);
		config.setRosterLoadedAtLogin(true);
		config.setReconnectionAllowed(true);
		connection = new XMPPConnection(config);
		
		try {
			Log.i("GETCONNECTION","connect");
			connection.connect();
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		
		return connection;
	}
	
	/** check validate of username 
	 * 
	 * @param username user name
	 * @return boolean return true if the username is validated, false if not*/
	private boolean validteUsername(String username) {
		String strUser = "^[a-zA-Z]|[0-9]|[a-zA-Z0-9]|[a-zA-Z0-9_]{5,20}";
		Pattern pUser = Pattern.compile(strUser);
		Matcher mUser = pUser.matcher(username);
		return mUser.matches();
	}
	/** check validate of email 
	 * 
	 * @param email user email
	 * @return boolean return true of the email is validated*/
	private boolean validteEmail(String email) {
		String strPattern = "^[a-zA-Z][\\w\\.-]*[a-zA-Z0-9]@[a-zA-Z0-9][\\w\\.-]*[a-zA-Z0-9]\\.[a-zA-Z][a-zA-Z\\.]*[a-zA-Z]$";
		Pattern pEmail = Pattern.compile(strPattern);
		Matcher mEmail = pEmail.matcher(email);
		return mEmail.matches();
	}

}
