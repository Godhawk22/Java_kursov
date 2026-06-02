import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Клиент запуска игры: одиночный режим или локальный матч на двух игроков. */
public class GameClient {
    public static void main(String[] args) {
        boolean localPvp = args.length > 0 && ("--local-pvp".equals(args[0]) || "--two-players".equals(args[0]));
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(localPvp ? "Tanky 1990 - Local PvP" : "Tanky 1990 - Java");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(new Tanky1990Game(localPvp));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
