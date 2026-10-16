package Agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class TaxiAgent extends Agent {

    private String taxiId;
    private String driverName;
    private boolean available;
    private double rating;
    private double basePrice;

    @Override
    protected void setup() {

        taxiId = getLocalName();

        // Récupération des arguments
        Object[] args = getArguments();

        if (args != null && args.length >= 3) {

            driverName = args[0].toString();
            rating = Double.parseDouble(args[1].toString());
            basePrice = Double.parseDouble(args[2].toString());

        } else {

            driverName = "Chauffeur";
            rating = 4.5;
            basePrice = 40.0;
        }

        available = true;

        // =====================================================
        // AFFICHAGE
        // =====================================================

        System.out.println();
        System.out.println("======================================");
        System.out.println("       NOUVEAU TAXI CONNECTE");
        System.out.println("======================================");

        System.out.println("Taxi       : " + taxiId);
        System.out.println("Chauffeur  : " + driverName);
        System.out.println("Note       : " + rating);
        System.out.println("Prix       : " + basePrice + " DH");
        System.out.println("Disponible : " + available);

        System.out.println("======================================");

        // Enregistrement dans le DF
        registerService();

        // Comportement d'écoute
        addBehaviour(new TaxiMessageBehaviour());
    }

    // =============================================================
    // ENREGISTREMENT DU SERVICE DANS LE DF
    // =============================================================

    private void registerService() {

        try {

            DFAgentDescription description =
                    new DFAgentDescription();

            description.setName(getAID());

            ServiceDescription service =
                    new ServiceDescription();

            service.setType("taxi-service");

            service.setName("Taxi-" + taxiId);

            description.addServices(service);

            DFService.register(
                    this,
                    description
            );

            System.out.println(
                    "[" + taxiId +
                    "] Service taxi enregistre dans le DF."
            );

        } catch (Exception e) {

            System.out.println(
                    "[" + taxiId +
                    "] Erreur enregistrement DF."
            );

            e.printStackTrace();
        }
    }

    // =============================================================
    // COMPORTEMENT PRINCIPAL
    // =============================================================

    private class TaxiMessageBehaviour
            extends CyclicBehaviour {

        @Override
        public void action() {

            ACLMessage message = receive();

            if (message == null) {

                block();
                return;
            }

            String sender =
                    message.getSender().getLocalName();

            System.out.println();
            System.out.println(
                    "[" + taxiId +
                    "] Message recu de : " +
                    sender
            );

            System.out.println(
                    "[" + taxiId +
                    "] Type : " +
                    ACLMessage.getPerformative(
                            message.getPerformative()
                    )
            );

            // =================================================
            // CFP
            // =================================================

            if (message.getPerformative()
                    == ACLMessage.CFP) {

                processReservationRequest(message);
            }

            // =================================================
            // ACCEPT_PROPOSAL
            // =================================================

            else if (message.getPerformative()
                    == ACLMessage.ACCEPT_PROPOSAL) {

                acceptReservation(message);
            }

            // =================================================
            // REJECT_PROPOSAL
            // =================================================

            else if (message.getPerformative()
                    == ACLMessage.REJECT_PROPOSAL) {

                System.out.println(
                        "[" + taxiId +
                        "] Proposition refusee."
                );
            }
        }
    }

    // =============================================================
    // TRAITEMENT DU CFP
    // =============================================================

    private void processReservationRequest(
            ACLMessage request) {

        ACLMessage response =
                request.createReply();

        // Vérification disponibilité
        if (!available) {

            response.setPerformative(
                    ACLMessage.REFUSE
            );

            response.setContent(
                    "Taxi indisponible"
            );

            send(response);

            System.out.println(
                    "[" + taxiId +
                    "] REFUSE : taxi indisponible."
            );

            return;
        }

        // =========================================================
        // PROPOSITION
        // =========================================================

        response.setPerformative(
                ACLMessage.PROPOSE
        );

        response.setContent(
                String.valueOf(basePrice)
        );

        send(response);

        System.out.println(
                "[" + taxiId +
                "] PROPOSE : " +
                basePrice +
                " DH"
        );
    }

    // =============================================================
    // ACCEPTATION DE LA RESERVATION
    // =============================================================

    private void acceptReservation(
            ACLMessage message) {

        // Le taxi devient indisponible
        available = false;

        System.out.println();
        System.out.println(
                "======================================"
        );

        System.out.println(
                "[" + taxiId +
                "] RESERVATION ACCEPTEE !"
        );

        System.out.println(
                "[" + taxiId +
                "] Chauffeur : " +
                driverName
        );

        System.out.println(
                "[" + taxiId +
                "] Prix : " +
                basePrice +
                " DH"
        );

        System.out.println(
                "[" + taxiId +
                "] Taxi maintenant indisponible."
        );

        System.out.println(
                "======================================"
        );

        // =========================================================
        // INFORMER LE BOOKING AGENT
        // =========================================================

        ACLMessage confirmation =
                message.createReply();

        confirmation.setPerformative(
                ACLMessage.INFORM
        );

        confirmation.setContent(
                "RESERVATION_CONFIRMEE" +
                "|Taxi=" + taxiId +
                "|Chauffeur=" + driverName +
                "|Prix=" + basePrice +
                "|Note=" + rating
        );

        send(confirmation);
    }

    // =============================================================
    // ARRET DU TAXI
    // =============================================================

    @Override
    protected void takeDown() {

        try {

            DFService.deregister(this);

        } catch (Exception e) {

            e.printStackTrace();
        }

        System.out.println(
                "[" + taxiId +
                "] Taxi arrete."
        );
    }
}