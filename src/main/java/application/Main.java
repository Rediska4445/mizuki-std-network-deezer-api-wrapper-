package main.java.application;

import main.java.deezer.Deezer;
import main.java.deezer.models.Track;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main
        extends JFrame
{
    private Deezer deezer = new Deezer();
    private ExecutorService executor = Executors.newFixedThreadPool(2);

    private JTabbedPane tabbedPane;
    private JList<Track> trackList;
    private DefaultListModel<Track> trackModel = new DefaultListModel<>();
    private JTextField artistField, trackField, queryField;
    private JLabel statusLabel;
    private JButton searchBtn, topBtn, chartBtn, relatedBtn, chainBtn;

    public Main() {
        setTitle("Deezer Similar Tracks Finder");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("Артист/Трек ID:"));
        artistField = new JTextField("DVRST", 12);
        topPanel.add(artistField);

        topPanel.add(new JLabel("Поиск:"));
        queryField = new JTextField("phonk dream", 12);  // ← ЭТОГО НЕ БЫЛО!
        topPanel.add(queryField);

        topPanel.add(new JLabel("Регион:"));
        JTextField regionField = new JTextField("ru", 6);

        statusLabel = new JLabel("Готов");
        statusLabel.setForeground(Color.BLUE);
        topPanel.add(statusLabel);

        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout());
        searchBtn = new JButton("🔍 Search");
        topBtn = new JButton("🎵 Top Artist");
        chartBtn = new JButton("📊 Charts");
        relatedBtn = new JButton("🔗 Related");
        chainBtn = new JButton("⛓️ Chain");

        buttonPanel.add(searchBtn);
        buttonPanel.add(topBtn);
        buttonPanel.add(chartBtn);
        buttonPanel.add(relatedBtn);
        buttonPanel.add(chainBtn);

        // Список треков
        trackList = new JList<>(trackModel);
        trackList.setCellRenderer(new TrackRenderer());
        trackList.addListSelectionListener(this::onTrackSelect);

        JScrollPane scroll = new JScrollPane(trackList);

        // Табы
        tabbedPane = new JTabbedPane();
        tabbedPane.add("Треки", scroll);
        tabbedPane.add("Лог", new JTextArea());

        // Layout
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
        add(tabbedPane, BorderLayout.CENTER);

        // События
        searchBtn.addActionListener(this::searchTracks);
        topBtn.addActionListener(e -> loadTopTracks());
        chartBtn.addActionListener(e -> loadCharts());
        relatedBtn.addActionListener(this::loadRelatedTracks);
        chainBtn.addActionListener(this::loadChain);
    }

    private void searchTracks(ActionEvent e) {
        executor.submit(() -> loadAsync(() -> {
            String query = queryField.getText();

            List<Track> tracks = null;
            try {
                tracks = deezer.searchTracks(query, 25);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
            updateList(tracks);
        }));
    }

    private void loadTopTracks() {
        executor.submit(() -> loadAsync(() -> {
            String artist = artistField.getText();
            List<Track> tracks = null;
            try {
                tracks = deezer.getTopTracksByArtist(artist, 25);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            updateList(tracks);
        }));
    }

    private void loadCharts() {
        executor.submit(() -> loadAsync(() -> {
            List<Track> tracks = null;
            try {
                tracks = deezer.getChartTracks("ru", 50);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            updateList(tracks);
        }));
    }

    private void loadRelatedTracks(ActionEvent e) {
        executor.submit(() -> loadAsync(() -> {
            String trackId = artistField.getText();  // используем как ID
            List<Track> tracks = null;
            try {
                tracks = deezer.getRelatedTracks(trackId, 5, 3, 5, 10, 0);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            updateList(tracks);
        }));
    }

    private void loadChain(ActionEvent e) {
        executor.submit(() -> loadAsync(() -> {
            String artist = artistField.getText();
            List<Track> tracks = null;
            try {
                tracks = deezer.getArtistChain(artist, 2, 2);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            updateList(tracks);
        }));
    }

    private void loadAsync(Runnable task) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("⏳ Загрузка...");
            statusLabel.setForeground(Color.ORANGE);
        });

        try {
            task.run();
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("❌ Ошибка: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
            });
            ex.printStackTrace();
        }
    }

    private void updateList(List<Track> tracks) {
        SwingUtilities.invokeLater(() -> {
            trackModel.clear();
            tracks.forEach(trackModel::addElement);
            statusLabel.setText("✅ " + tracks.size() + " треков");
            statusLabel.setForeground(Color.GREEN);
        });
    }

    private void onTrackSelect(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            Track selected = trackList.getSelectedValue();
            if (selected != null) {
                System.out.println("Выбран: " + selected.getTitle_short() +
                        " - " + selected.getArtist().getName());
            }
        }
    }

    private static class TrackRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Track track) {
                String text = String.format("%s - %s (%d)",
                        track.getArtist().getName(),
                        track.getTitle_short(),
                        track.getDuration() / 60);
                setText(text);
                setToolTipText(track.getPreview());
            }

            return this;
        }
    }

    public static void main(String[] args) throws Exception {
//        SwingUtilities.invokeLater(() -> {
//            try {
//                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//            } catch (Exception ignored) {}
//
//            new Main().setVisible(true);
//        });

        Deezer deezer1 = new Deezer();
        System.out.println(deezer1.getRelatedTracks(
                "Dvrst - dream space", 6, 6, 6, 0, 3
        ));
    }
}
