import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;

public class MobileDevice {
    //class to have contacts
    static class Contacts{
        String hash;
        int date, duration;

        public Contacts(String individual, int date, int duration){
            this.hash = individual;
            this.date = date;
            this.duration = duration;
        }
    }
    //variables required for the mobile device
    String address,deviceName,hash;
    ArrayList<Contacts> contacts;
    ArrayList<String> testHashes;
    Government gov;

    //constructor
    public MobileDevice(String configFile, Government contactTracer) throws IOException, NoSuchAlgorithmException {
        //read a property file to create hash of mobile device
        try(FileReader file = new FileReader(configFile)) {
            Properties p = new Properties();
            p.load(file);
            this.address = p.getProperty("address");
            this.deviceName = p.getProperty("deviceName");
        }catch(NullPointerException n){
            n.printStackTrace();
            System.out.println("There was an error while reading the config file for mobile device!");
        }catch(Exception e){
            e.printStackTrace();
        }

        try {
            //creating hash
            byte[] address = this.address.getBytes(StandardCharsets.UTF_8);
            byte[] deviceName = this.deviceName.getBytes(StandardCharsets.UTF_8);
            //Creating the MessageDigest object
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            //Passing data to the created MessageDigest Object
            md.update(address);
            md.update(deviceName);
            //Compute the message digest
            byte[] digest = md.digest();
            //Converting the byte array in to HexString format
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < digest.length; i++) {
                hexString.append(Integer.toHexString(0xFF & digest[i]));
            }
        this.hash = hexString.toString();
        }catch (NullPointerException n){
//            n.printStackTrace();
            System.out.println("Threre was some error in the content of config file!");
        }
        //initializing other class variables
        this.contacts = new ArrayList<>();
        this.gov = contactTracer;
        testHashes = new ArrayList<>();
    }
    //record contacts
    public void recordContact(String individual, int date, int duration){
        //add contacts to arraylist of contacts
        try {
            Contacts c = new Contacts(individual, date, duration);
            contacts.add(c);
        }catch (Exception e){
            System.out.println("Some issue came up while recording the contact!");
            e.printStackTrace();
        }
    }
    //mark positive test and respective testhash
    public void positiveTest(String testHash){
        //add testhashes to its arraylist
        try {
            testHashes.add(testHash);
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Some problem with testhashes record!");
        }
    }
    //sync data with government database and report back positive contacts if any
    public boolean synchronizeData() throws SQLException {
        //take a string for storing all the contact info
        String contactInfo = "";
        try {
            //concat th information in a single string
            for (Contacts contact : contacts) {
                contactInfo = contactInfo.concat(contact.hash);
                contactInfo = contactInfo.concat("/");
                contactInfo = contactInfo.concat(String.valueOf(contact.date));
                contactInfo = contactInfo.concat("/");
                contactInfo = contactInfo.concat(String.valueOf(contact.duration));
                contactInfo = contactInfo.concat("\n");
            }
            //append testhashes of this device to the end of contactInfo string
            for (String testHash : testHashes) {
                contactInfo = contactInfo.concat(testHash);
                contactInfo = contactInfo.concat("$$");
            }
            //call government method to sync data
            return gov.mobileContact(this.hash,contactInfo);
        }catch (Exception e){
            System.out.println("Some error occurred while synchronizing the data and could not be further checked whether mobile device has been in contact or not!");
            return false;
        }

    }

}
