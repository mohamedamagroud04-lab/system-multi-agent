package Agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingAgent extends Agent {

    private List<AID> taxiAgents;

    // Associe une réservation à un client
    private Map<String, AID> clients;

    @Override
    protected void setup() {

        taxiAgents = new ArrayList<>();
        clients = new HashMap<>();

        System.out.println();
        System.out.println("======================================");
        System.out.println("       BOOKING AGENT DEMARRE");
        System.out.println("======================================");

        // Enregistrement du BookingAgent dans le DF
        registerService();

        // Recherche régulière des taxis
        addBehaviour(
                new SearchTaxiBehaviour(
                        this,
                        1000
                )
        );

        // Attente des demandes clients
        addBehaviour(
                new BookingMessageBehaviour()
        );
    }

    // =============================================================
    // ENREGISTREMENT DU BOOKING SERVICE
    // =============================================================

    private void registerService() {

        try {

            DFAgentDescription description =
                    new DFAgentDescription();

            description.setName(getAID());

            ServiceDescription service =
                    new ServiceDescription();

            service.setType("booking-service");

            service.setName("Booking-Service");

            description.addServices(service);

            DFService.register(
                    this,
                    description
            );

            System.out.println(
                    "[Booking] Service booking enregistre dans le DF."
            );

        } catch (Exception e) {

            System.out.println(
                    "[Booking] Erreur enregistrement DF."
            );

            e.printStackTrace();
        }
    }

    // =============================================================
    // RECHERCHE DES TAXIS
    // =============================================================

    private void searchTaxiAgents() {

        try {

            DFAgentDescription template =
                    new DFAgentDescription();

            ServiceDescription service =
                    new ServiceDescription();

            service.setType("taxi-service");

            template.addServices(service);

            DFAgentDescription[] results =
                    DFService.search(
                            this,
                            template
                    );

            // Nouvelle liste pour éviter les doublons
            List<AID> newTaxiList =
                    new ArrayList<>();

            for (DFAgentDescription result : results) {

                AID taxi =
                        result.getName();

                if (!newTaxiList.contains(taxi)) {

                    newTaxiList.add(taxi);
                }
            }

            taxiAgents = newTaxiList;

            System.out.println();
            System.out.println(
                    "[Booking] Taxis trouves : "
                            + taxiAgents.size()
            );

            for (AID taxi : taxiAgents) {

                System.out.println(
                        "[Booking] -> "
                                + taxi.getLocalName()
                );
            }

        } catch (Exception e) {

            System.out.println(
                    "[Booking] Erreur recherche taxis."
            );
        }
    }

    // =============================================================
    // RECHERCHE PERIODIQUE DES TAXIS
    // =============================================================

    private class SearchTaxiBehaviour
            extends TickerBehaviour {

        public SearchTaxiBehaviour(
                Agent agent,
                long period) {

            super(agent, period);
        }

        @Override
        protected void onTick() {

            searchTaxiAgents();

            // Si les 3 taxis sont trouvés
            if (taxiAgents.size() >= 3) {

                System.out.println();
                System.out.println(
                        "[Booking] Tous les taxis sont disponibles."
                );

                // Arrêt de la recherche répétitive
                stop();
            }
        }
    }

    // =============================================================
    // ATTENTE DES REQUEST DES CLIENTS
    // =============================================================

    private class BookingMessageBehaviour
            extends CyclicBehaviour {

        @Override
        public void action() {

            ACLMessage message =
                    receive();

            if (message == null) {

                block();
                return;
            }

            System.out.println();
            System.out.println(
                    "[Booking] Message recu de : "
                            + message.getSender()
                            .getLocalName()
            );

            System.out.println(
                    "[Booking] Type : "
                            + ACLMessage.getPerformative(
                            message.getPerformative()
                    )
            );

            // =====================================================
            // REQUEST CLIENT
            // =====================================================

            if (message.getPerformative()
                    == ACLMessage.REQUEST) {

                handleClientRequest(message);
            }

            // =====================================================
            // INFORM DU TAXI
            // =====================================================

            else if (message.getPerformative()
                    == ACLMessage.INFORM) {

                handleTaxiConfirmation(message);
            }
        }
    }

    // =============================================================
    // TRAITEMENT DE LA DEMANDE CLIENT
    // =============================================================

    private void handleClientRequest(
            ACLMessage clientRequest) {

        AID client =
                clientRequest.getSender();

        String conversationId =
                clientRequest.getConversationId();

        System.out.println();
        System.out.println(
                "======================================"
        );

        System.out.println(
                "[Booking] NOUVELLE RESERVATION"
        );

        System.out.println(
                "[Booking] Client : "
                        + client.getLocalName()
        );

        System.out.println(
                "[Booking] Trajet : "
                        + clientRequest.getContent()
        );

        System.out.println(
                "======================================"
        );

        // Vérification des taxis
        searchTaxiAgents();

        if (taxiAgents.isEmpty()) {

            ACLMessage failure =
                    clientRequest.createReply();

            failure.setPerformative(
                    ACLMessage.FAILURE
            );

            failure.setContent(
                    "Aucun taxi disponible."
            );

            send(failure);

            return;
        }

        // Mémoriser le client
        clients.put(
                conversationId,
                client
        );

        // Envoyer CFP
        sendCFP(
                clientRequest.getContent(),
                conversationId
        );
    }

    // =============================================================
    // ENVOI DU CFP
    // =============================================================

    private void sendCFP(
            String trip,
            String conversationId) {

        ACLMessage cfp =
                new ACLMessage(
                        ACLMessage.CFP
                );

        for (AID taxi : taxiAgents) {

            cfp.addReceiver(taxi);
        }

        cfp.setContent(trip);

        cfp.setConversationId(
                conversationId
        );

        send(cfp);

        System.out.println();
        System.out.println(
                "[Booking] CFP envoye aux "
                        + taxiAgents.size()
                        + " taxis."
        );

        // Ajouter le comportement qui attend les propositions
        addBehaviour(
                new ReceiveProposalsBehaviour(
                        conversationId
                )
        );
    }

    // =============================================================
    // RECEPTION DES PROPOSITIONS
    // =============================================================

    private class ReceiveProposalsBehaviour
            extends CyclicBehaviour {

        private String conversationId;

        private List<AID> proposers =
                new ArrayList<>();

        private List<Double> prices =
                new ArrayList<>();

        private int expectedResponses;

        public ReceiveProposalsBehaviour(
                String conversationId) {

            this.conversationId =
                    conversationId;

            expectedResponses =
                    taxiAgents.size();
        }

        @Override
        public void action() {

            MessageTemplate template =
                    MessageTemplate.and(

                            MessageTemplate.MatchPerformative(
                                    ACLMessage.PROPOSE
                            ),

                            MessageTemplate.MatchConversationId(
                                    conversationId
                            )
                    );

            ACLMessage message =
                    receive(template);

            if (message == null) {

                block();
                return;
            }

            try {

                double price =
                        Double.parseDouble(
                                message.getContent()
                        );

                proposers.add(
                        message.getSender()
                );

                prices.add(price);

                System.out.println(
                        "[Booking] PROPOSE de "
                                + message.getSender()
                                .getLocalName()
                                + " : "
                                + price
                                + " DH"
                );

                // Toutes les réponses reçues
                if (proposers.size()
                        >= expectedResponses) {

                    selectBestTaxi();

                    myAgent.removeBehaviour(
                            this
                    );
                }

            } catch (Exception e) {

                System.out.println(
                        "[Booking] Prix invalide."
                );
            }
        }

        // =========================================================
        // SELECTION DU MEILLEUR TAXI
        // =========================================================

        private void selectBestTaxi() {

            if (proposers.isEmpty()) {

                System.out.println(
                        "[Booking] Aucune proposition."
                );

                return;
            }

            int bestIndex = 0;

            for (int i = 1;
                 i < prices.size();
                 i++) {

                if (prices.get(i)
                        < prices.get(bestIndex)) {

                    bestIndex = i;
                }
            }

            AID selectedTaxi =
                    proposers.get(bestIndex);

            double bestPrice =
                    prices.get(bestIndex);

            System.out.println();
            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "[Booking] TAXI SELECTIONNE"
            );

            System.out.println(
                    "[Booking] Taxi : "
                            + selectedTaxi.getLocalName()
            );

            System.out.println(
                    "[Booking] Prix : "
                            + bestPrice
                            + " DH"
            );

            System.out.println(
                    "======================================"
            );

            // =====================================================
            // ACCEPTATION
            // =====================================================

            ACLMessage accept =
                    new ACLMessage(
                            ACLMessage.ACCEPT_PROPOSAL
                    );

            accept.addReceiver(
                    selectedTaxi
            );

            accept.setConversationId(
                    conversationId
            );

            accept.setContent(
                    "Reservation acceptee"
            );

            send(accept);

            System.out.println(
                    "[Booking] ACCEPT_PROPOSAL -> "
                            + selectedTaxi.getLocalName()
            );

            // =====================================================
            // REFUS DES AUTRES TAXIS
            // =====================================================

            for (AID taxi : proposers) {

                if (!taxi.equals(
                        selectedTaxi)) {

                    ACLMessage reject =
                            new ACLMessage(
                                    ACLMessage.REJECT_PROPOSAL
                            );

                    reject.addReceiver(taxi);

                    reject.setConversationId(
                            conversationId
                    );

                    reject.setContent(
                            "Un autre taxi a ete selectionne."
                    );

                    send(reject);

                    System.out.println(
                            "[Booking] REJECT_PROPOSAL -> "
                                    + taxi.getLocalName()
                    );
                }
            }
        }
    }

    // =============================================================
    // CONFIRMATION DU TAXI
    // =============================================================

    private void handleTaxiConfirmation(
            ACLMessage message) {

        String content =
                message.getContent();

        // Vérifier qu'il s'agit bien d'une réservation
        if (!content.startsWith(
                "RESERVATION_CONFIRMEE")) {

            return;
        }

        String conversationId =
                message.getConversationId();

        AID client =
                clients.get(conversationId);

        if (client == null) {

            System.out.println(
                    "[Booking] Client introuvable."
            );

            return;
        }

        System.out.println();
        System.out.println(
                "[Booking] Confirmation recue du taxi."
        );

        // =========================================================
        // INFORM CLIENT
        // =========================================================

        ACLMessage confirmation =
                new ACLMessage(
                        ACLMessage.INFORM
                );

        confirmation.addReceiver(client);

        confirmation.setConversationId(
                conversationId
        );

        confirmation.setContent(
                content
        );

        send(confirmation);

        System.out.println(
                "[Booking] INFORM -> "
                        + client.getLocalName()
        );

        // Supprimer la réservation terminée
        clients.remove(
                conversationId
        );
    }

    // =============================================================
    // ARRET
    // =============================================================

    @Override
    protected void takeDown() {

        try {

            DFService.deregister(this);

        } catch (Exception e) {

            e.printStackTrace();
        }

        System.out.println(
                "[Booking] BookingAgent arrete."
        );
    }
}