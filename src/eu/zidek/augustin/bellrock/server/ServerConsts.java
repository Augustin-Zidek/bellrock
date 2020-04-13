package eu.zidek.augustin.bellrock.server;

import eu.zidek.augustin.bellrock.identification.MsgConstants;

/**
 * Constants for the server.
 * 
 * @author Augustin Zidek
 *
 */
public class ServerConsts {

	// Cipher settings
	static final String CIPHER_NAME = "AES";
	static final int KEY_LENGTH = 128;
	

	// Decryption settings
	static final int LAST_SEEN_USER_CACHE_SIZE = 1000;
	
	
	// Key Store file
	static final String KEY_STORE_PATH = "key_store.ks";
	static final char[] KS_PSWD_GLOBAL = new char[] { 't', 'u', 'c', 'n', '4',
			'k', 's', '3', 'd', 'i', 'n', 'a', 'p', 'o', 'l', '1' };
	static final char[] KS_PSWD_LOCAL = new char[] { 's', 'e', 'd', 'i', 'k',
			'a', '8', 'e', 'l', 'p', '0', 'd', 'd', 'u', 'b', '3', 'm' };
	
	
	// Cell tower locations database
	@SuppressWarnings("javadoc")
	public static final String CELL_TOWER_LOCATIONS_FILE = "cell_towers_db/cell_towers_hashmap.dat";
	
	// Database
	static final String DB_PATH = "bellrock_database";
	static final int UID_HEX_STR_LEN = MsgConstants.UID_LENGTH * 2;
	static final int AID_HEX_STR_LEN = MsgConstants.AID_LENGTH * 2;
	// Buffering settings
	static final long DATABASE_AUTO_COMMIT_TIME_MS = 1_000 * 5; // 5 seconds
	static final int COMMIT_BUFFER_SIZE = 5_000;
	// Database: Create tables
	static final String SQL_CREATE_UIDS_TABLE = 
			String.format("CREATE TABLE IF NOT EXISTS Uids("
						+ "Uid CHAR(%d) NOT NULL, PRIMARY KEY (Uid))", UID_HEX_STR_LEN);
	static final String SQL_CREATE_PEERS_TABLE = 
			String.format("CREATE TABLE IF NOT EXISTS Peers("
						+ "Uid CHAR(%d) NOT NULL, "
						+ "Peer CHAR(%d) NOT NULL)", UID_HEX_STR_LEN, UID_HEX_STR_LEN);
	static final String SQL_CREATE_OBSERVATIONS_TABLE = 
			String.format("CREATE TABLE IF NOT EXISTS Observations("
						+ "Uid CHAR(%d) NOT NULL, "
						+ "Aid CHAR(%d) NOT NULL, "
						+ "DecryptedUid CHAR(%d), "
						+ "Timestamp BIGINT, "
						+ "Lat DOUBLE, "
						+ "Lon DOUBLE, "
						+ "PosStr VARCHAR(255))", UID_HEX_STR_LEN, AID_HEX_STR_LEN, UID_HEX_STR_LEN);
	static final String SQL_CREATE_LOCATIONS_TABLE = 
			String.format("CREATE TABLE IF NOT EXISTS Locations("
						+ "Uid CHAR(%d) NOT NULL, "
						+ "StartTime BIGINT, "
						+ "EndTime BIGINT, "
						+ "Lat FLOAT, "
						+ "Lon FLOAT,"
						+ "CellTowerInfo BIGINT)", UID_HEX_STR_LEN);
	// Database: User management
	static final String SQL_ADD_USER = "INSERT INTO Uids(Uid) VALUES (?)";
	static final String SQL_DEL_USER_FROM_UID = "DELETE FROM Uids WHERE Uid = ?";
	static final String SQL_DEL_USER_FROM_PEERS = "DELETE FROM Peers WHERE Uid = ? OR Peer = ?";
	static final String SQL_DEL_USER_FROM_OBSERVATIONS = "DELETE FROM Observations WHERE Uid = ? OR DecryptedUid = ?";
	static final String SQL_GET_ALL_UIDS = "SELECT Uid FROM Uids";
	static final String SQL_CONTAINS_UID = "IF EXISTS (SELECT * FROM Uids WHERE Uid = ?)"
										 + "BEGIN RETURN 1 END"
										 + "ELSE BEGIN RETURN 0 END";
	// Database: Peer management
	static final String SQL_ADD_PEER = "INSERT INTO Peers VALUES (?,?)";
	static final String SQL_DEL_PEER = "DELETE FROM Peers WHERE "
									+ "((Uid = ? AND Peer = ?) OR (Uid = ? AND Peer = ?))";
	static final String SQL_GET_PEERS = "SELECT Peer FROM Peers WHERE Uid = ?";
	// Database: Observation management
	static final String SQL_ADD_OBSERVATION = "INSERT INTO Observations VALUES (?,?,?,?,?,?,?)";
	static final String SQL_DEL_OBSERVATION = "DELETE FROM Observations WHERE "
			+ "Uid = ? AND Aid = ? AND DecryptedUid = ? AND Timestamp = ? AND Lat = ? AND Lon = ? AND PosStr = ?";
	static final String SQL_GET_OBSERVATIONS = "SELECT * FROM Observations WHERE Uid = ?";
	// Database: Location management
    static final String SQL_ADD_LOCATION = "INSERT INTO Locations VALUES (?,?,?,?,?,?)";
	static final String SQL_GET_LOCATIONS = "SELECT StartTime, EndTime, Lat, Lon, CellTowerInfo FROM Locations "
										  + "WHERE Uid = ?";
	static final String SQL_GET_USERS_AT_LOCATION = "SELECT Uid FROM Locations WHERE "
			+ "Lat = ? AND Lon = ? AND StartTime <= ? AND EndTime >= ?";
	// Starting point or the ending point lies within the given interval. 
	// Feed with values (LAT, LON, START, START, START, START)
    static final String SQL_GET_USERS_AT_LOCATION_AT_INTERVAL = "SELECT Uid FROM Locations "
            + "WHERE Lat = ? AND Lon = ? AND NOT((StartTime < ? AND EndTime < ?) OR (StartTime > ? AND EndTime > ?))";
	// Starting point or the ending point lies within the given interval. 
	// Feed with values (UID, START, START, START, START)
	static final String SQL_GET_LOCATIONS_IN_INTERVAL = "SELECT StartTime, EndTime, Lat, Lon, CellTowerInfo FROM Locations "
			  + "WHERE Uid = ? AND NOT((StartTime < ? AND EndTime < ?) OR (StartTime > ? AND EndTime > ?))";
	// Database: Global commands
	static final String SQL_CLEAR_ALL = "TRUNCATE SCHEMA PUBLIC RESTART IDENTITY AND COMMIT NO CHECK";
	static final String SQL_SHUT_DOWN = "SHUTDOWN";
	
	
	// Key Store DB
	static final int BASE_64_KEY_LENGTH = 24;
	static final String KEY_STORE_DB_PATH = "bellrock_keystore";
	static final String KEY_STORE_DB_KEY = "EDEBBFDB2D97D455B27A2A29AACED109";
	static final String SQL_CREATE_KEY_STORE_TABLE = String.format("CREATE TABLE IF NOT EXISTS KeyStore("
			+ "Uid CHAR(%d) NOT NULL, "
			+ "Key CHAR(%d) NOT NULL)", 
			UID_HEX_STR_LEN, BASE_64_KEY_LENGTH);
	static final String SQL_ADD_KEY = "INSERT INTO KeyStore VALUES (?,?)";
	static final String SQL_DEL_KEY = "DELETE FROM KeyStore WHERE Uid = ?";
	static final String SQL_GET_KEY = "SELECT Key FROM KeyStore WHERE Uid = ?";
	static final String SQL_GET_ALL_USER_KEY_MAP = "SELECT * FROM KeyStore";

}
