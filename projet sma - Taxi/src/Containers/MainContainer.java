package Containers;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class MainContainer {

    public static void main(String[] args) {

        System.out.println();
        System.out.println("============================================");
        System.out.println("      SYSTEME MULTI-AGENTS TAXI");
        System.out.println("============================================");

        try {

            // Création du Runtime JADE
            Runtime runtime = Runtime.instance();

            // Configuration du Main Container
            ProfileImpl profile = new ProfileImpl();
            profile.setParameter(Profile.GUI, "true");

            // Création du Main Container
            AgentContainer mainContainer =
                    runtime.createMainContainer(profile);

            System.out.println();
            System.out.println("[SYSTEM] Main Container cree.");

            // ==============================
            // CREATION DU BOOKING AGENT
            // ==============================

            AgentController bookingAgent =
                    mainContainer.createNewAgent(
                            "BookingAgent",
                            "Agents.BookingAgent",
                            null
                    );

            bookingAgent.start();

            // ==============================
            // CREATION DES TAXIS
            // ==============================

            createTaxiAgent(
                    mainContainer,
                    "Taxi_1",
                    "Ahmed",
                    4.8
            );

            createTaxiAgent(
                    mainContainer,
                    "Taxi_2",
                    "Youssef",
                    4.5
            );

            createTaxiAgent(
                    mainContainer,
                    "Taxi_3",
                    "Omar",
                    4.7
            );

            // ==============================
            // CREATION D'UN SEUL CLIENT
            // ==============================

            createClientAgent(
                    mainContainer,
                    "Client_1",
                    "Agdal",
                    "Hay Riad"
            );

            System.out.println();
            System.out.println("============================================");
            System.out.println("[SYSTEM] Tous les agents ont ete lances.");
            System.out.println("============================================");

        } catch (Exception e) {

            System.out.println(
                    "[SYSTEM] Erreur lors du demarrage de JADE."
            );

            e.printStackTrace();
        }
    }

    // ==========================================
    // CREATION D'UN TAXI
    // ==========================================

    private static void createTaxiAgent(
            AgentContainer container,
            String name,
            String driver,
            double rating) {

        try {

            Object[] arguments =
                    new Object[]{
                            driver,
                            rating
                    };

            AgentController agent =
                    container.createNewAgent(
                            name,
                            "Agents.TaxiAgent",
                            arguments
                    );

            agent.start();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // ==========================================
    // CREATION D'UN CLIENT
    // ==========================================

    private static void createClientAgent(
            AgentContainer container,
            String name,
            String departure,
            String destination) {

        try {

            Object[] arguments =
                    new Object[]{
                            departure,
                            destination
                    };

            AgentController agent =
                    container.createNewAgent(
                            name,
                            "Agents.ClientAgent",
                            arguments
                    );

            agent.start();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}