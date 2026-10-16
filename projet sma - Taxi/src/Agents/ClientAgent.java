package Agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;

public class ClientAgent extends Agent {

    private String departure;
    private String destination;

    private boolean requestSent = false;

    @Override
    protected void setup() {

        Object[] args = getArguments();

        if (args != null && args.length >= 2) {

            departure = args[0].toString();
            destination = args[1].toString();

        } else {

            departure = "Centre Ville";
            destination = "Universite";
        }

        System.out.println();
        System.out.println(
                "======================================"
        );

        System.out.println(
                "          CLIENT AGENT DEMARRE"
        );

        System.out.println(
                "======================================"
        );

        System.out.println(
                "Client       : "
                        + getLocalName()
        );

        System.out.println(
                "Depart       : "
                        + departure
        );

        System.out.println(
                "Destination  : "
                        + destination
        );

        System.out.println(
                "======================================"
        );

        // Recherche répétitive du BookingAgent
        addBehaviour(
                new SearchBookingBehaviour(
                        this,
                        1000
                )
        );

        // Attente de la réponse
        addBehaviour(
                new ClientResponseBehaviour()
        );
    }

    // =============================================================
    // RECHERCHE DU BOOKING AGENT
    // =============================================================

    private class SearchBookingBehaviour
            extends TickerBehaviour {

        public SearchBookingBehaviour(
                Agent agent,
                long period) {

            super(agent, period);
        }

        @Override
        protected void onTick() {

            // Si demande déjà envoyée
            if (requestSent) {

                stop();
                return;
            }

            try {

                DFAgentDescription template =
                        new DFAgentDescription();

                ServiceDescription service =
                        new ServiceDescription();

                service.setType(
                        "booking-service"
                );

                template.addServices(service);

                DFAgentDescription[] results =
                        DFService.search(
                                myAgent,
                                template
                        );

                // =================================================
                // BOOKING AGENT TROUVE
                // =================================================

                if (results.length > 0) {

                    AID bookingAgent =
                            results[0].getName();

                    System.out.println();
                    System.out.println(
                            "[Client] BookingAgent trouve : "
                                    + bookingAgent.getLocalName()
                    );

                    sendRequest(
                            bookingAgent
                    );

                    requestSent = true;

                    stop();
                }

                else {

                    System.out.println(
                            "[Client] Recherche du BookingAgent..."
                    );
                }

            } catch (Exception e) {

                System.out.println(
                        "[Client] Erreur recherche BookingAgent."
                );
            }
        }
    }

    // =============================================================
    // ENVOI DE LA DEMANDE
    // =============================================================

    private void sendRequest(
            AID bookingAgent) {

        ACLMessage request =
                new ACLMessage(
                        ACLMessage.REQUEST
                );

        request.addReceiver(
                bookingAgent
        );

        // Identifiant unique de réservation
        String conversationId =
                getLocalName()
                        + "-"
                        + System.currentTimeMillis();

        request.setConversationId(
                conversationId
        );

        request.setContent(
                "Depart : "
                        + departure
                        + " | Destination : "
                        + destination
        );

        send(request);

        System.out.println();
        System.out.println(
                "--------------------------------------"
        );

        System.out.println(
                "[Client] REQUEST envoye."
        );

        System.out.println(
                "[Client] Vers : "
                        + bookingAgent.getLocalName()
        );

        System.out.println(
                "[Client] Trajet : "
                        + departure
                        + " -> "
                        + destination
        );

        System.out.println(
                "--------------------------------------"
        );
    }

    // =============================================================
    // RECEPTION DE LA REPONSE
    // =============================================================

    private class ClientResponseBehaviour
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

            // =====================================================
            // RESERVATION CONFIRMEE
            // =====================================================

            if (message.getPerformative()
                    == ACLMessage.INFORM) {

                System.out.println(
                        "======================================"
                );

                System.out.println(
                        "       RESERVATION CONFIRMEE"
                );

                System.out.println(
                        "======================================"
                );

                System.out.println(
                        "[Client] "
                                + message.getContent()
                );

                System.out.println(
                        "======================================"
                );
            }

            // =====================================================
            // ECHEC
            // =====================================================

            else if (message.getPerformative()
                    == ACLMessage.FAILURE) {

                System.out.println(
                        "======================================"
                );

                System.out.println(
                        "[Client] RESERVATION ECHOUEE"
                );

                System.out.println(
                        "[Client] "
                                + message.getContent()
                );

                System.out.println(
                        "======================================"
                );
            }
        }
    }
}