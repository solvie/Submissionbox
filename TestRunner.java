import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class TestRunner {
   public static void main(String[] args) {
      Result result = JUnitCore.runClasses(TestJunit.class);

      //System.out.println("Was successful? "+ result.wasSuccessful());
       int passcount =result.getRunCount()-result.getFailureCount();
      System.out.println("PASSRATE:@ "+passcount+"/" +result.getRunCount()+"@ ");
       for (Failure failure : result.getFailures()) {
           //System.out.println("Hark! A failure!");
           System.out.println(failure.toString());
       }
   }
}  
