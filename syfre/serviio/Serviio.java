package syfre.serviio;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.*;
import java.util.regex.*;

///////////////////////////////////////////////////////////////////////////////
// SERVIIO OBJECT
///////////////////////////////////////////////////////////////////////////////

class ServiioObject {
   private boolean verbose;
   private Connection conn;
   private String table;
   private String column;
   private int id;
   private String value;

   public ServiioObject(Connection iconn, String iTable, String iColumn, int iID, String iValue) {
      conn = iconn;
      table = iTable;
      column = iColumn;
      id = iID;
      value = iValue;
   }

   public Connection getConn() {return conn; }
   public boolean getVerbose() { return verbose; }
   public void setVerbose(boolean value) { verbose = value;  }
   public String getValue() { return value; }
   public void setValue(String newval) { value = newval; }
   public int getID() { return id; }
   public void setID(int newid) { id = newid; }
   public String debugStr() { return getID() + " : " + getValue(); }
   public void logMessage(String aMessage) { if (getVerbose()) System.out.println(aMessage); }

   static public String normalizeName(String name) {
      String s = name.trim();
      int index;
      for (index = s.length() - 1; index > 0; index--) {
         if ("0123456789".indexOf(s.charAt(index)) == -1) {
            break;
         }
      }
      return s.substring(0, index + 1).trim();
   }

   public int doExecuteSelectID(PreparedStatement ps) throws SQLException {

      ResultSet rs = ps.executeQuery();
      int val = 0;
      if (rs.next()) {
         val = rs.getInt("ID");
      }
      rs.close();
      return val;
   }

   public boolean find() throws SQLException {

      if (getID() > 0)
         return true;

      String query = "SELECT ID FROM " + table + " WHERE " + column + "=?";
      PreparedStatement p = conn.prepareStatement(query);
      p.setString(1, getValue());

      setID(doExecuteSelectID(p));
      return getID() > 0;
   }

   public PreparedStatement doPrepareCreate(Connection conn) throws SQLException {

      String query = "INSERT INTO " + table + " (" + column + ") VALUES (?)";
      PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, getValue());
      return ps;
   }

   public int doExecuteInsert(PreparedStatement ps) throws SQLException {
      ps.execute();
      ResultSet rs = ps.getGeneratedKeys();
      int val = 0;
      if (rs.next()) {
         val = rs.getInt(1);
      }
      rs.close();
      return val;
   }

   public boolean create() throws SQLException {

      if (getID() > 0)
         return true;

      PreparedStatement ps = doPrepareCreate(conn);
      setID(doExecuteInsert(ps));
      return getID() > 0;
   }

   public boolean resolve(boolean iCreate) throws SQLException {
      find();
      if (iCreate)
         create();
      return getID() > 0;
   }
}

///////////////////////////////////////////////////////////////////////////////
// ACTOR
///////////////////////////////////////////////////////////////////////////////

class Actor extends ServiioObject {

   public Actor(Connection iconn, int iID, String iName) throws SQLException {
      super(iconn, "PERSON", "NAME", iID, iName);
      find();
   }

   public String getName() { return getValue(); }
   public String getSortName() { return getName(); }
   public String getInitial() { return getName().substring(0, 1); }

   public PreparedStatement doPrepareCreate(Connection conn) throws SQLException {

      String query = "INSERT INTO PERSON (NAME,SORT_NAME,INITIAL) VALUES (?,?,?)";
      PreparedStatement ps = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, getName());
      ps.setString(2, getSortName());
      ps.setString(3, getInitial());
      return ps;
   }
}

///////////////////////////////////////////////////////////////////////////////
// GENRE
///////////////////////////////////////////////////////////////////////////////

class Genre extends ServiioObject { 
   public Genre(Connection iconn, int iID, String name) {
      super(iconn, "GENRE", "NAME", iID, name);

   }
}

///////////////////////////////////////////////////////////////////////////////
// MOVIE
///////////////////////////////////////////////////////////////////////////////

class Movie extends ServiioObject {

   private File file;
   private String[] parts;

   public Movie(Connection iconn, int iID, String filePath) throws SQLException {
      super(iconn, "MEDIA_ITEM", "FILE_PATH", iID, filePath);

      file = new File(filePath);
      parts = file.getName().split("-");
      find();
   }

   public String getName() { return file.getName(); }
   public String getStudio() { return parts[0].trim();  }
   public String getGenre() { return parts[0].trim(); }

   public String[] getActor() {

      String s = normalizeName(parts[1]);
      if (s.equals("E"))
           return parts[2].trim().split(",");
      else return parts[1].trim().split(",");
   }

   public int getLinkActor(Actor actor) throws SQLException {

      if ((getID() == 0) || (actor.getID() == 0))
         return 0;

      String query = "SELECT ID FROM PERSON_ROLE WHERE PERSON_ID=? and MEDIA_ITEM_ID=?";
      PreparedStatement ps = getConn().prepareStatement(query);
      ps.setInt(1, actor.getID());
      ps.setInt(2, getID());
 
      return(doExecuteSelectID(ps));
   }

   public int linkActor(Actor actor) throws SQLException {

      if ((getID() == 0) || (actor.getID() == 0))
         return 0;

      //logMessage(String.format("Link actor:%s (%d) to movie:%d", actor.getValue(), actor.getID(), getID()));

      String query = "INSERT INTO PERSON_ROLE (ROLE_TYPE,PERSON_ID,MEDIA_ITEM_ID) VALUES ('ACTOR',?,?)";
      PreparedStatement ps = getConn().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
      ps.setInt(1, actor.getID());
      ps.setInt(2, getID());

      return doExecuteInsert(ps);
   }

   public int getLinkGenre(Genre genre) throws SQLException {

      if ((getID() == 0) || (genre.getID() == 0))
         return 0;

      String query = "SELECT ID FROM GENRE_ITEM WHERE GENRE_ID=? and MEDIA_ITEM_ID=?";
      PreparedStatement ps = getConn().prepareStatement(query);
      ps.setInt(1, genre.getID());
      ps.setInt(2, getID());
      //
      return(doExecuteSelectID(ps));
   }

   public int linkGenre(Genre genre) throws SQLException {

      if ((getID() == 0) || (genre.getID() == 0))
         return 0;

      //logMessage(String.format("Link genre:%s (%d) to movie:%d", genre.getValue(), genre.getID(), getID()));

      String query = "INSERT INTO GENRE_ITEM (GENRE_ID,MEDIA_ITEM_ID) VALUES (?,?)";
      PreparedStatement ps = getConn().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
      ps.setInt(1, genre.getID());
      ps.setInt(2, getID());

      return doExecuteInsert(ps);
   }
}

///////////////////////////////////////////////////////////////////////////////
// SERVIIO
///////////////////////////////////////////////////////////////////////////////

public class Serviio {
   private String serviioPath;
   private String databasePath;
   private String databaseSave;
   private boolean verbose;
   private boolean write;
   private Connection fconn;
   private int movieCount = 0;
   private int movieMissing = 0;
   private int actorMissing = 0;
   private int genreMissing = 0;
   private boolean serviioWasRunning = false;
   private Dictionary<String, Movie> movies = new Hashtable<String, Movie>();
   private Dictionary<String, Actor> actors = new Hashtable<String, Actor>();
   private Dictionary<String, Genre> genres = new Hashtable<String, Genre>();

   public Serviio(String iServiioPath) {
      serviioPath = iServiioPath;
      databasePath = serviioPath+"/db";
      databaseSave = serviioPath+"/dbsave";
   }

   public void connect() throws SQLException, ClassNotFoundException {
      // Registering the driver
      Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
      // Getting the Connection object
      String URL = "jdbc:derby:" + databasePath;
      if (getVerbose()) System.out.println(String.format("Connect to database:%s",URL));
      fconn = DriverManager.getConnection(URL);
   }

   public void disconnect() throws SQLException {
      conn().close();
   }

   public Connection conn() { return fconn; };
   public boolean getVerbose() { return verbose; }
   public void setVerbose(boolean value) { verbose = value; }
   public boolean getWrite() { return write; }
   public void setWrite(boolean value) { write = value; }
   public void logMessage(String aMessage) { if (getVerbose()) System.out.println(aMessage); }
   //
   public int getActorCount() { return actors.size(); }
   public int getGenreCount() { return genres.size(); }
   public int getMovieCount() { return movieCount; }
   public int getActorMissing() { return actorMissing; }
   public int getGenreMissing() { return genreMissing; }
   public int getMovieMissing() { return movieMissing; }
   public int getResolvedCount() { return movies.size(); }
   public int getUnResolvedCount() { return movieCount - movies.size(); }

   public void clean() throws SQLException {

      if (!getWrite())
         return;

      logMessage("Clean up");

      String query = "DELETE FROM PERSON_ROLE";
      PreparedStatement ps = conn().prepareStatement(query);
      ps.execute();

      query = "DELETE FROM PERSON";
      ps = conn().prepareStatement(query);
      ps.execute();

      query = "DELETE FROM GENRE_ITEM";
      ps = conn().prepareStatement(query);
      ps.execute();

      query = "DELETE FROM GENRE";
      ps = conn().prepareStatement(query);
      ps.execute();

      query = "UPDATE MEDIA_ITEM SET TITLE=FILE_NAME, SORT_TITLE=FILE_NAME WHERE TITLE<>FILE_NAME";
      ps = conn().prepareStatement(query);
      ps.execute();
   }

   public void addActor(Actor actor) {

      actor.setVerbose(getVerbose());
      actors.put(actor.getValue(), actor);
   }

   public void addGenre(Genre genre) {

      genre.setVerbose(getVerbose());
      genres.put(genre.getValue(), genre);
   }

   public void addMovie(Movie movie) {

      movieCount++;
      movie.setVerbose(getVerbose());
      movies.put(movie.getValue(), movie);
   }

   public void addMovie(String filePath) throws SQLException {

      Movie movie = movies.get(filePath);

      if (movie == null) {
         movie = new Movie(conn(), 0, filePath);
         addMovie(movie);
      }
      //
      if (movie.getID() > 0) {

         //logMessage(movie.debugStr());

         for (String actorName : movie.getActor()) {

            actorName = Actor.normalizeName(actorName);
            Actor actor = actors.get(actorName);
            if (actor == null) {

               actorMissing++;

               actor = new Actor(conn(), 0, actorName);
               addActor(actor);
               if (getWrite()) actor.resolve(true);

               logMessage(String.format("Add actor: '%s' %d", actor.getValue(), actor.getID()));
            }

            if ((actor != null) && (getWrite()) && (movie.getLinkActor(actor) == 0)) {
               movie.linkActor(actor);
            }
         }

         String genreName = movie.getGenre();
         Genre genre = genres.get(genreName);
         if (genre==null) {

            genreMissing++;

            genre = new Genre(conn(), 0, genreName);
            addGenre(genre);
            if (getWrite()) genre.resolve(true);
            logMessage(String.format("Add genre: '%s' %d", genre.getValue(), genre.getID()));
         }
         if ((genre != null) && (getWrite()) && (movie.getLinkGenre(genre) == 0)) {
            movie.linkGenre(genre);
         }
      }
      else {
         movieMissing++;
         logMessage(String.format("Movie not found '%s'",filePath));
      }
   }

   public void loadMovies() throws SQLException {

      String query = "SELECT ID, FILE_PATH FROM MEDIA_ITEM";
      PreparedStatement p = conn().prepareStatement(query);

      ResultSet rs = p.executeQuery();
      while (rs.next()) {
         Movie movie = new Movie(conn(), rs.getInt("ID"), rs.getString("FILE_PATH"));
         addMovie(movie);
      }
      rs.close();
   }

   public void loadActors() throws SQLException {

      String query = "SELECT ID, NAME FROM PERSON";
      PreparedStatement p = conn().prepareStatement(query);

      ResultSet rs = p.executeQuery();
      while (rs.next()) {
         Actor actor = new Actor(conn(), rs.getInt("ID"), rs.getString("NAME"));
         addActor(actor);
      }
      rs.close();
   }

   public void loadGenres() throws SQLException {

      String query = "SELECT ID, NAME FROM GENRE";
      PreparedStatement p = conn().prepareStatement(query);

      ResultSet rs = p.executeQuery();
      while (rs.next()) {
         Genre genre = new Genre(conn(), rs.getInt("ID"), rs.getString("NAME"));
         addGenre(genre);
      }
      rs.close();
   }

   public void load() throws SQLException {
      loadMovies();
      loadActors();
      loadGenres();
   }

   public void printStats() {
      System.out.println(String.format("Total movies:%d (%d missing), actors:%d (%d missing), genres:%d (%d missing)",
            getMovieCount(),
            getMovieMissing(),
            getActorCount(),
            getActorMissing(),
            getGenreCount(),
            getGenreMissing()
            ));

   }

   //////////////////////////////////////////////////////////////

   public void scan(final String pattern, final File folder) throws SQLException {

      logMessage(String.format("Scan directory:%s", folder.getPath()));

      for (final File f : folder.listFiles()) {

         if (f.isDirectory()) {
            scan(pattern, f);
         }
         if (f.isFile()) {
            if (f.getName().matches(pattern)) {
               addMovie(f.getPath());
            }
         }
      }
   }

   /////////////////////////////////////////////////////////////////

   boolean isProcessRunning(String processName) throws IOException {

      // Running command that will get all the working processes.
      String line;
      Process proc = Runtime.getRuntime().exec("ps -ef");
      InputStream stream = proc.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      //Parsing the input stream.
      while ((line = reader.readLine()) != null) {
          Pattern pattern = Pattern.compile(processName);
          Matcher matcher = pattern.matcher(line);
          if (matcher.find()) {
              return true;
          }
      }
      return false;
   }

   void execCommand(String debugStr, String cmd) throws IOException, InterruptedException {
      logMessage(debugStr);
      Process p = Runtime.getRuntime().exec(cmd);
      p.waitFor();
   }

   public void stopServiioService() throws IOException, InterruptedException, ClassNotFoundException, SQLException, Exception {

      final String cServiio = "org.serviio.MediaServer";

      if (isProcessRunning(cServiio)) {

         logMessage("stopping serviio");
         serviioWasRunning = true;

         execCommand("Stop service", "systemctl stop serviio");

         int ctn = 0;
         while (isProcessRunning(cServiio) && (ctn<10)) {
            Thread.sleep(1000);
            ctn++;
         }
         if (isProcessRunning(cServiio)) {
            logMessage("Stoping serviio failed, abort");
            throw new Exception("Stoping service failed");
         }
      } else {
         logMessage("serviio not running");
      }
   }
   public void startServiioService() throws IOException, InterruptedException {
      
      if (serviioWasRunning) {
         execCommand("Start service", "systemctl start serviio");
      }
   }

   public void start() throws IOException, InterruptedException, ClassNotFoundException, SQLException, Exception  {

      if (!getWrite()) {
         connect();
         return;
      }

      stopServiioService();

      execCommand("Cleanup backup copy", "rm -r "+databaseSave);
      execCommand("Make backup copy", "cp -r "+databasePath+" "+databaseSave);
      //not a good idea, better to run under root
      //execCommand("Remove lock","rm -f "+databasePath+"/db.lck");
      //
      connect();
  }

  public void stop() throws IOException, InterruptedException, SQLException {

      disconnect();

      if (getWrite()) {
         startServiioService();
      }
  }

}
