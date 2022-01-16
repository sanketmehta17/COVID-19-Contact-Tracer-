import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, SQLException {

        //instantiate the government object
        Government g = new Government("configFile");
        //instantiate the mobie device object1
        MobileDevice user = new MobileDevice("filepath", g);
        //instantiate the mobile device object2
        MobileDevice individual = new MobileDevice("filepath", g);
        //record contact of individual with user
        user.recordContact(individual.hash,7,10);
        //record positive test of user having testhash as with the givernment
        user.positiveTest("covidTest1");
        g.recordTestResult("covidTest1",15,true);

        //record contact of individual wit user which is not required
        individual.recordContact(user.hash,7,10);
        //record individual's positive test
        individual.positiveTest("covidIndividual1");
        g.recordTestResult("covidIndividual1",10,true);
        //report whether individual has come into contact with other device having positive result and sync individual's data
        System.out.println("Individual has been in contact or not: "+individual.synchronizeData());
        //report whether user has come into contact with other device having positive result and sync individual's data
        System.out.println("User has been in contact or not: "+user.synchronizeData());
        //find large gatherings
        System.out.println(g.findGatherings(7,3,5,1));


    }
}
