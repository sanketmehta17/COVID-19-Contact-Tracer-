import java.io.FileReader;
import java.sql.*;
import java.util.*;

public class Government {

    //variables required for government class
    String database,user,password;
    Connection connection;

    //constructor
    public Government(String configFile) {
        //read a property file and store the config information
        try(FileReader file = new FileReader(configFile)) {
            Properties p = new Properties();
            p.load(file);
            this.database = p.getProperty(" database");
            this.user = p.getProperty("user");
            this.password = p.getProperty("password");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Some error occurred while reading the config file for government!");
        }
        //make a connection to the government database
        try {
            this.connection = DriverManager.getConnection("jdbc:mysql://db.cs.dal.ca:3306/sumehta", "sumehta", "B00881783");
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("Error connecting to database!");
        }
    }
    //syncing data with database and report positive contacts of calling device
    public boolean mobileContact(String initiator, String contactInfo) throws SQLException {

        //to insert mobile device and contactinfo from a separate method to keep functionality separate
        insertData(initiator, contactInfo);

        //check positive contacts within 14 days
        String query = "select t.testhash from TestResult t join ContactInfo c \n" +
                "    where c.chash = t.mhash\n" +
                "    and c.mhash = \"" + initiator + "\"\n" +
                "    and abs(c.date - t.date) < 14" +
                "    and t.notified = false;";
        try(Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery(query);
            int count = 0;
            //if there are more than 1 testhashes then increment the count
            ArrayList<String> testHashes = new ArrayList<>();
            while (rs.next()) {
                testHashes.add(rs.getString("testhash"));
                count++;
            }

            //if count > 0 then update the testhashes and link device hash with them in database
            if (count > 0) {
                //to insert notified=true into testResult table whichever testhashes have been notified
                StringBuilder string = new StringBuilder();
                for (int i = 0; i < testHashes.size(); i++) {
                    String testHash = testHashes.get(i);
                    if (testHashes.size() > 1 && i < testHashes.size() - 1) {
                        string.append("testhash = ").append("\"").append(testHash).append("\"").append(" ").append("or").append(" ");
                    } else {
                        string.append("testhash = ").append("\"").append(testHash).append("\"");
                    }
                }
                Statement s0 = connection.createStatement();
                s0.executeUpdate("update ignore TestResult set notified = true where " + string + " ;");
                return true;
            } else {
                //return false if there are no such positive contacts found
                return false;
            }
        }catch(Exception e){
            System.out.println("Some error occurred while checking for contact with other positive contacts!");
            return false;
        }
    }

    public void recordTestResult(String testHash, int date, boolean result) throws SQLException {
        try {
            //insert the test recirds into the database
            PreparedStatement ps = connection.prepareStatement("INSERT IGNORE INTO TestResult(testhash,mhash,date,result,notified)" +
                    "VALUES(?,?,?,?,?)");
            ps.setString(1, testHash);
            //pass device hash as null which will be updated later when it is synced
            ps.setString(2, null);
            ps.setString(3, String.valueOf(date));
            ps.setBoolean(4, result);
            //set by default the field notified = false
            ps.setBoolean(5, false);
            ps.executeUpdate();
        }catch(SQLException e){
            e.printStackTrace();
            System.out.println("Some error occurred while storing test result in database!");
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("Some error occurred while storing test result in database!");
        }
    }

    public int findGatherings(int date, int minSize, int minTime, float density) throws SQLException {

        //create a set of arraylist for holding all the pairs of individuals required
        Set<ArrayList<String>> pairs = new HashSet<>();
        //create a map for storing the common device as key and value as a gathering to which that person has been into
        Map<String,Set<String>> individuals = new HashMap<>();
        //query to get those pairs of individuals
        String sql = "select mhash,chash from ContactInfo where " +
                "date = ? group by mhash,chash " +
                "having sum(duration) >= ? ;";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, String.valueOf(date));
            ps.setInt(2, minTime);
            ResultSet rs = ps.executeQuery();

            //storing all the pairs into arraylist
            while (rs.next()) {
                ArrayList<String> pairOfIndividuals = new ArrayList<>();
                pairOfIndividuals.add(rs.getString("mhash"));
                pairOfIndividuals.add(rs.getString("chash"));
                //adding this pair into the set of pairs
                pairs.add(pairOfIndividuals);
            }
        }catch (SQLException e){
            e.printStackTrace();
            System.out.println("Error while getting pairs of indiciduals from database!");
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Error while getting pairs of indiciduals from database!");
        }

        try {
            //checking each pari with other pairs for getting a gathering with common individuals
            for (ArrayList<String> set : pairs) {

                Set<String> temp = new HashSet<>(set);
                for (ArrayList<String> pair : pairs) {
                    Set<String> intersection = new HashSet<>(temp);
                    Set<String> temp1 = new HashSet<>(pair);
                    if (temp1.equals(temp)) {
                        continue;
                    }
                    if (pair.contains(set.get(0)) || pair.contains(set.get(1))) {
                        intersection.retainAll(temp1);
                        temp1.addAll(temp);
                    } else {
                        intersection.clear();
                        continue;
                    }

                    if (individuals.containsKey(intersection.toString())) {
                        individuals.get(intersection.toString()).addAll(temp1);
                    } else {
                        individuals.put(intersection.toString(), temp1);
                    }
                }
            }
            for (ArrayList<String> set : pairs) {
                Set<String> xyz1 = new HashSet<>(set);
                for (String s : set) {
                    if (individuals.containsKey("[" + s + "]")) {
                        continue;
                    } else {
                        individuals.put("[" + s + "]", xyz1);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        try {
            //gatherings
            Set<Set<String>> gatherings = new HashSet<>(individuals.values());
            //store number of individuals as size of gatherings
            int NumOfIndividuals = gatherings.size();

            //array list of final pairs
            ArrayList<Set<String>> finalPairs = new ArrayList<>(gatherings);
            //remove the ineligible gatherings
            for (int i = 0; i < finalPairs.size(); i++) {
                if (!(finalPairs.get(i).size() >= minSize)) {
                    finalPairs.remove(finalPairs.get(i));
                }
            }
            //do the caluculation and return large gathering if any
            int n = NumOfIndividuals;
            float c = pairs.size();
            int m = (n * (n - 1)) / 2;
            if (c / m > density) {
                return finalPairs.size();
            } else {
                return 0;
            }
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("Error while calculating final gatherings pair!");
            return 0;
        }
    }

    private void insertData(String initiator, String contactInfo) throws SQLException {

        try {
            //split via different delimeters which contains contactinfo and testhashes
            String[] arr = contactInfo.split("\n");
            ArrayList<String> abc = new ArrayList<>();
            if (arr[arr.length - 1].contains("$$")) {
                abc.addAll(Arrays.asList(arr).subList(0, arr.length - 1));
            } else {
                Collections.addAll(abc, arr);
            }

            //insert into mobile device
            PreparedStatement md = connection.prepareStatement("INSERT IGNORE INTO MobileDevice(mhash) values(?)");
            md.setString(1, initiator);
            md.executeUpdate();

            for (String s : abc) {
                //insert contact into mobile device
                String[] example = s.split("/");
                PreparedStatement md1 = connection.prepareStatement("INSERT IGNORE INTO MobileDevice(mhash) values(?)");
                md1.setString(1, example[0]);
                md1.executeUpdate();

                //insert into contact info table
                PreparedStatement ps = connection.prepareStatement("INSERT IGNORE INTO ContactInfo(mhash,chash,date,duration) values(?,?,?,?)");
                String[] example1 = s.split("/");
                //sets parameter values.
                ps.setString(1, initiator);
                ps.setString(2, example1[0]);
                ps.setString(3, example1[1]);
                ps.setString(4, example1[2]);
                //executes query.
                ps.executeUpdate();

                PreparedStatement ps1 = connection.prepareStatement("INSERT IGNORE INTO ContactInfo(mhash,chash,date,duration) values(?,?,?,?)");
                //sets parameter values.
                ps1.setString(1, example1[0]);
                ps1.setString(2, initiator);
                ps1.setString(3, example1[1]);
                ps1.setString(4, example1[2]);
                //executes query.
                ps1.executeUpdate();
            }
            //to insert mhash into testResult table
            if (arr[arr.length - 1].contains("$$")) {
                String[] testHashes = arr[arr.length - 1].split("\\$\\$");
                StringBuilder string = new StringBuilder();
                for (int i = 0; i < testHashes.length; i++) {
                    String testHash = testHashes[i];
                    if (testHashes.length > 1 && i < testHashes.length - 1) {
                        string.append("testhash = ").append("\"").append(testHash).append("\"").append(" ").append("or").append(" ");
                    } else {
                        string.append("testhash = ").append("\"").append(testHash).append("\"");
                    }
                }
                Statement s0 = connection.createStatement();
                s0.executeUpdate("update ignore TestResult set mhash = \"" + initiator + "\" where " + string + " ;");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error occured in sql or contactinfo !");
        }catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error occured in sql or contactinfo !");
        }
    }

}
