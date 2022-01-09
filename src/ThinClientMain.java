import java.net.URL;
import java.rmi.RMISecurityManager;
import java.rmi.server.RMIClassLoader;

public class ThinClientMain {

    public static void main(String[]Args){
        if (Args.length == 0) {
            System.err.println("Usage: java LoadClient <remote URL>");
            System.exit(-1);
        }
        System.setProperty("java.security.policy", "MyGrantAllPolicy.policy");
        if (System.getSecurityManager() == null) System.setSecurityManager(new RMISecurityManager());
        try {
            URL url = new URL(Args[0]);
            String client_configuration = null;
            if (Args.length >= 2) client_configuration = Args[1];
            Class<?> client_Class = RMIClassLoader.loadClass(url, "LoadClient");
            Runnable client = (Runnable) client_Class.getDeclaredConstructor(String.class).newInstance(client_configuration);
            client.run();
        }
        catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}