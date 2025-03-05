package battleship;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class App {
    public static void main(String[] args) {
        try {
            // Try to create the RMI registry at port 1099
            try {
                LocateRegistry.createRegistry(1099); // Start RMI registry
                System.out.println("RMI Registry created on port 1099");
            } catch (RemoteException e) {
                System.out.println("RMI Registry already running.");
            }

            // Keep the registry alive by putting the main thread to sleep indefinitely
            // This will prevent the JVM from exiting and keep the registry alive
            Thread.sleep(Long.MAX_VALUE);

        } catch (InterruptedException e) {
        }
    }
}