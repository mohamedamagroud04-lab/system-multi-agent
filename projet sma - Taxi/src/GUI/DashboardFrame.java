package GUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DashboardFrame extends JFrame {

    // Composants accessibles pour les mises à jour dynamiques
    private JLabel clientInfoLabel;
    private JLabel routeLabel;
    private JTextArea logTextArea;
    private JLabel reservationStatusLabel;

    // Choix du trajet
    private JComboBox<String> departBox, arriveeBox;

    // ----------------------------------------------------------------------
    //  DONNÉES : lieux (coordonnées approximatives en km) et taxis
    // ----------------------------------------------------------------------
    private static final String[] LIEUX = {
        "Agdal", "Hay Riad", "Gare de Rabat Ville", "Aéroport Rabat-Salé",
        "Hassan", "Souissi", "Océan", "Médina", "Yacoub El Mansour"
    };
    // Positions relatives (x, y) en km - approximatives, à ajuster si besoin
    private static final double[][] COORDS = {
        { 0.0,  0.0},   // Agdal
        {-3.5, -3.5},   // Hay Riad
        { 1.5,  2.0},   // Gare de Rabat Ville
        { 9.0,  7.0},   // Aéroport Rabat-Salé
        { 3.0,  4.5},   // Hassan
        { 1.5, -3.0},   // Souissi
        { 3.5,  5.5},   // Océan
        { 4.0,  5.0},   // Médina
        {-4.0,  3.0}    // Yacoub El Mansour
    };

    private static final String[] TAXI_IDS = {"Taxi_1", "Taxi_2", "Taxi_3"};
    private static final String[] DRIVERS  = {"Ahmed", "Youssef", "Omar"};
    private static final String[] RATINGS  = {"4.8", "4.5", "4.7"};
    // Tarif = base + km * tarifKm (chaque taxi a ses propres tarifs)
    private static final double[] BASE     = {8.0, 5.0, 10.0};
    private static final double[] PER_KM   = {2.2, 2.0, 1.6};

    private final JLabel[] priceLabels   = new JLabel[3];
    private final JLabel[] priceCaptions = new JLabel[3];
    private final double[] prices        = new double[3];

    // ----------------------------------------------------------------------
    //  DESIGN SYSTEM
    // ----------------------------------------------------------------------
    private static final Color BG        = new Color(0xF3, 0xF5, 0xF9);
    private static final Color NAVY      = new Color(0x14, 0x21, 0x35);
    private static final Color BLUE      = new Color(0x3B, 0x82, 0xF6);
    private static final Color GREEN     = new Color(0x15, 0xA2, 0x4A);
    private static final Color GREEN_BG  = new Color(0xE6, 0xF7, 0xEC);
    private static final Color ORANGE    = new Color(0xD9, 0x77, 0x06);
    private static final Color TEXT      = new Color(0x1E, 0x29, 0x3B);
    private static final Color MUTED     = new Color(0x6B, 0x72, 0x80);

    private static final String FONT = Font.SANS_SERIF;
    private static final Font F_TITLE = new Font(FONT, Font.BOLD, 26);
    private static final Font F_SUB   = new Font(FONT, Font.PLAIN, 13);
    private static final Font F_H2    = new Font(FONT, Font.BOLD, 17);
    private static final Font F_H3    = new Font(FONT, Font.BOLD, 14);
    private static final Font F_BODY  = new Font(FONT, Font.PLAIN, 13);
    private static final Font F_CHIP  = new Font(FONT, Font.BOLD, 12);

    public DashboardFrame() {
        setTitle("Smart Taxi - Multi-Agent System Dashboard");
        setSize(1000, 1000);
        setMinimumSize(new Dimension(900, 900));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);

        updateRoute(); // calcule trajet + prix + journal au démarrage
    }

    // ----------------------------------------------------------------------
    //  EN-TÊTE
    // ----------------------------------------------------------------------
    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(22, 28, 20, 28));

        JLabel title = new JLabel("SMART TAXI");
        title.setFont(F_TITLE);
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("Système de réservation multi-agents  •  JADE / ACL");
        sub.setFont(F_SUB);
        sub.setForeground(new Color(0x9C, 0xB3, 0xD4));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);
        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        right.setOpaque(false);
        right.add(chip("●  RESERVATION CONFIRMED", GREEN, GREEN_BG));
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ----------------------------------------------------------------------
    //  CORPS DE LA PAGE
    // ----------------------------------------------------------------------
    private JComponent buildBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(20, 28, 24, 28));

        body.add(buildClientCard());
        body.add(Box.createVerticalStrut(14));
        body.add(buildTripCard());
        body.add(Box.createVerticalStrut(14));
        body.add(buildTaxiRow());
        body.add(Box.createVerticalStrut(14));
        body.add(buildCommCard());      // seule carte qui s'étire
        body.add(Box.createVerticalStrut(14));
        body.add(buildActionBar());
        return body;
    }

    // --- Carte client ------------------------------------------------------
    private JComponent buildClientCard() {
        RoundedPanel card = new RoundedPanel(18, Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 22, 16, 22));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));

        clientInfoLabel = new JLabel("CLIENT_1");
        clientInfoLabel.setFont(F_H2);
        clientInfoLabel.setForeground(TEXT);

        routeLabel = new JLabel("");
        routeLabel.setFont(F_BODY);
        routeLabel.setForeground(MUTED);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(clientInfoLabel);
        left.add(Box.createVerticalStrut(6));
        left.add(routeLabel);

        card.add(left, BorderLayout.WEST);
        card.add(chip("BookingAgent", BLUE, new Color(0xEB, 0xF2, 0xFE)), BorderLayout.EAST);
        return card;
    }

    // --- Carte trajet ------------------------------------------------------
    private JComponent buildTripCard() {
        RoundedPanel card = new RoundedPanel(18, Color.WHITE);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(16, 22, 16, 22));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JLabel title = new JLabel("TRAJET DU CLIENT");
        title.setFont(F_H3);
        title.setForeground(TEXT);
        card.add(title, BorderLayout.NORTH);

        departBox  = styledCombo(0);   // Agdal
        arriveeBox = styledCombo(1);   // Hay Riad

        JButton swap = new JButton("⇄");
        swap.setFont(new Font(FONT, Font.BOLD, 16));
        swap.setForeground(BLUE);
        swap.setFocusPainted(false);
        swap.setContentAreaFilled(false);
        swap.setBorder(new EmptyBorder(4, 10, 4, 10));
        swap.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        swap.setToolTipText("Inverser départ et arrivée");
        swap.addActionListener(e -> {
            Object d = departBox.getSelectedItem();
            departBox.setSelectedItem(arriveeBox.getSelectedItem());
            arriveeBox.setSelectedItem(d);
        });

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.weightx = 1; c.insets = new Insets(0, 0, 0, 8);
        row.add(labeled("DÉPART", departBox), c);

        c.gridx = 1; c.weightx = 0; c.insets = new Insets(14, 0, 0, 0);
        row.add(swap, c);

        c.gridx = 2; c.weightx = 1; c.insets = new Insets(0, 8, 0, 0);
        row.add(labeled("ARRIVÉE (DESTINATION)", arriveeBox), c);

        card.add(row, BorderLayout.CENTER);

        // À chaque changement : on recalcule trajet, prix et journal
        departBox.addActionListener(e -> updateRoute());
        arriveeBox.addActionListener(e -> updateRoute());
        return card;
    }

    private JComboBox<String> styledCombo(int selectedIndex) {
        JComboBox<String> box = new JComboBox<>(LIEUX);
        box.setSelectedIndex(selectedIndex);
        box.setFont(F_BODY);
        box.setBackground(Color.WHITE);
        box.setForeground(TEXT);
        box.setPreferredSize(new Dimension(200, 34));
        return box;
    }

    private JComponent labeled(String caption, JComponent field) {
        JLabel l = new JLabel(caption);
        l.setFont(F_CHIP);
        l.setForeground(MUTED);
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    // ----------------------------------------------------------------------
    //  LOGIQUE : distance, prix, mise à jour de l'interface
    // ----------------------------------------------------------------------
    /** Distance approximative en km (ligne droite x 1.3 pour tenir compte des routes). */
    private double distanceKm(int from, int to) {
        double dx = COORDS[from][0] - COORDS[to][0];
        double dy = COORDS[from][1] - COORDS[to][1];
        return Math.max(1.0, Math.hypot(dx, dy) * 1.3);
    }

    /** Recalcule le trajet affiché, les prix des taxis et le journal. */
    private void updateRoute() {
        if (departBox == null || arriveeBox == null || logTextArea == null) return;

        int from = departBox.getSelectedIndex();
        int to   = arriveeBox.getSelectedIndex();
        double km = distanceKm(from, to);

        routeLabel.setText(LIEUX[from] + "   ➔   " + LIEUX[to]
                + String.format("   •   %.1f km", km));

        // Prix de chaque taxi
        int best = 0;
        for (int i = 0; i < 3; i++) {
            prices[i] = Math.round(BASE[i] + km * PER_KM[i]);
            if (prices[i] < prices[best]) best = i;
        }
        for (int i = 0; i < 3; i++) {
            boolean isBest = (i == best);
            priceLabels[i].setText(String.format("%.0f MAD", prices[i]));
            priceLabels[i].setForeground(isBest ? GREEN : TEXT);
            priceCaptions[i].setText(isBest ? "✓ Meilleure offre" : "Offre");
            priceCaptions[i].setForeground(isBest ? GREEN : MUTED);
        }

        logTextArea.setText(buildLog(best));
        // Affiche la fin de la conversation (la confirmation).
        // Remplace par setCaretPosition(0) pour lire depuis le début.
        logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        reservationStatusLabel.setText("START RESERVATION");
    }

    /** Génère le journal de négociation à partir des prix actuels. */
    private String buildLog(int best) {
        StringBuilder sb = new StringBuilder();
        sb.append("CLIENT_1 → BookingAgent : REQUEST (")
          .append(getDepart()).append(" ➔ ").append(getArrivee()).append(")\n");
        for (String id : TAXI_IDS) {
            sb.append("BookingAgent → ").append(id).append(" : CFP\n");
        }
        for (int i = 0; i < 3; i++) {
            sb.append(String.format("%s → BookingAgent : PROPOSE  prix = %.0f MAD\n",
                    TAXI_IDS[i], prices[i]));
        }
        sb.append("BookingAgent : Comparaison des offres...\n");
        sb.append(String.format("BookingAgent : %s sélectionné (meilleur prix : %.0f MAD)\n",
                TAXI_IDS[best], prices[best]));
        sb.append("BookingAgent → ").append(TAXI_IDS[best]).append(" : ACCEPT_PROPOSAL\n");
        for (int i = 0; i < 3; i++) {
            if (i != best) {
                sb.append("BookingAgent → ").append(TAXI_IDS[i]).append(" : REJECT_PROPOSAL\n");
            }
        }
        sb.append(TAXI_IDS[best]).append(" → BookingAgent : INFORM (course confirmée)\n");
        sb.append(String.format("BookingAgent → Client_1 : CONFIRMED  %s à %.0f MAD\n",
                TAXI_IDS[best], prices[best]));
        return sb.toString();
    }

    // Getters utilisables par le ClientAgent / BookingAgent
    public String getDepart()  { return (String) departBox.getSelectedItem(); }
    public String getArrivee() { return (String) arriveeBox.getSelectedItem(); }

    // --- Rangée des taxis ---------------------------------------------------
    private JComponent buildTaxiRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        for (int i = 0; i < 3; i++) {
            row.add(taxiCard(i));
        }
        return row;
    }

    /** Carte taxi : les labels de prix sont gardés pour être mis à jour. */
    private JComponent taxiCard(int i) {
        RoundedPanel card = new RoundedPanel(18, Color.WHITE);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel name = new JLabel(TAXI_IDS[i]);
        name.setFont(F_H2);
        name.setForeground(TEXT);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(name, BorderLayout.WEST);
        top.add(chip("AVAILABLE", GREEN, GREEN_BG), BorderLayout.EAST);

        JLabel drv = new JLabel("Chauffeur : " + DRIVERS[i]);
        drv.setFont(F_BODY);
        drv.setForeground(MUTED);
        drv.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rate = new JLabel("★  " + RATINGS[i]);
        rate.setFont(new Font(FONT, Font.BOLD, 13));
        rate.setForeground(ORANGE);

        priceLabels[i] = new JLabel("-- MAD");
        priceLabels[i].setFont(new Font(FONT, Font.BOLD, 20));
        priceLabels[i].setForeground(TEXT);

        priceCaptions[i] = new JLabel("Offre");
        priceCaptions[i].setFont(F_CHIP);
        priceCaptions[i].setForeground(MUTED);

        JPanel priceBox = new JPanel();
        priceBox.setOpaque(false);
        priceBox.setLayout(new BoxLayout(priceBox, BoxLayout.Y_AXIS));
        priceBox.add(priceLabels[i]);
        priceBox.add(priceCaptions[i]);

        JPanel mid = new JPanel(new BorderLayout());
        mid.setOpaque(false);
        mid.setAlignmentX(Component.LEFT_ALIGNMENT);
        mid.add(rate, BorderLayout.WEST);
        mid.add(priceBox, BorderLayout.EAST);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.add(top);
        col.add(Box.createVerticalStrut(10));
        col.add(drv);
        col.add(Box.createVerticalStrut(12));
        col.add(mid);

        card.add(col, BorderLayout.NORTH);
        return card;
    }

    // --- Journal de communication (grand, extensible) -----------------------
    private JComponent buildCommCard() {
        RoundedPanel card = new RoundedPanel(18, Color.WHITE);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(18, 22, 18, 22));
        card.setPreferredSize(new Dimension(100, 380));   // hauteur confortable
        card.setMinimumSize(new Dimension(100, 300));     // ne s'écrase plus
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE)); // peut s'étirer

        JLabel title = new JLabel("COMMUNICATION DES AGENTS");
        title.setFont(F_H3);
        title.setForeground(TEXT);
        card.add(title, BorderLayout.NORTH);

        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);
        logTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        logTextArea.setForeground(TEXT);
        logTextArea.setOpaque(false);
        logTextArea.setMargin(new Insets(4, 0, 4, 0));

        JScrollPane scroll = new JScrollPane(logTextArea);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // --- Barre d'action ------------------------------------------------------
    private JComponent buildActionBar() {
        RoundedPanel bar = new RoundedPanel(18, BLUE);
        bar.setLayout(new BorderLayout());
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        bar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        reservationStatusLabel = new JLabel("START RESERVATION", SwingConstants.CENTER);
        reservationStatusLabel.setFont(new Font(FONT, Font.BOLD, 15));
        reservationStatusLabel.setForeground(Color.WHITE);
        reservationStatusLabel.setBorder(new EmptyBorder(14, 0, 14, 0));
        bar.add(reservationStatusLabel, BorderLayout.CENTER);

        bar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                startReservation();
            }
        });
        return bar;
    }

    /** Valide le trajet et confirme la réservation avec le meilleur prix. */
    private void startReservation() {
        if (departBox.getSelectedItem().equals(arriveeBox.getSelectedItem())) {
            JOptionPane.showMessageDialog(this,
                    "Le départ et l'arrivée doivent être différents.",
                    "Trajet invalide", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int best = 0;
        for (int i = 1; i < 3; i++) if (prices[i] < prices[best]) best = i;

        reservationStatusLabel.setText(String.format("RÉSERVATION CONFIRMÉE : %s à %.0f MAD",
                TAXI_IDS[best], prices[best]));

        // TODO : envoyer getDepart() / getArrivee() au ClientAgent JADE
        // ex. : content = getDepart() + ";" + getArrivee()
    }

    // ----------------------------------------------------------------------
    //  UTILITAIRES DE DESIGN
    // ----------------------------------------------------------------------
    private JComponent chip(String text, Color fg, Color bg) {
        RoundedPanel p = new RoundedPanel(14, bg);
        p.setLayout(new BorderLayout());
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(F_CHIP);
        l.setForeground(fg);
        l.setBorder(new EmptyBorder(5, 12, 5, 12));
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bg;

        RoundedPanel(int radius, Color bg) {
            this.radius = radius;
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DashboardFrame dashboard = new DashboardFrame();
            dashboard.setVisible(true);
        });
    }
}