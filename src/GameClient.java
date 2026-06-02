import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Клиент запуска матча с выбором режима игры и условия окончания. */
public class GameClient {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameStartOptions options = askStartOptions();
            JFrame frame = new JFrame(options.playerCount == 1 ? "Tanky 1990 - Single Player" : "Tanky 1990 - Local PvP");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.setContentPane(new Tanky1990Game(options.playerCount, options.finishByScore, options.targetScore));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static GameStartOptions askStartOptions() {
        String[] playerOptions = {"1 игрок", "2 игрока"};
        int playerChoice = JOptionPane.showOptionDialog(null,
                "Выберите режим игры", "Tanky 1990",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, playerOptions, playerOptions[1]);
        int playerCount = playerChoice == 0 ? 1 : 2;

        String[] finishOptions = {"До очков или смерти игроков", "Только до смерти игроков"};
        int finishChoice = JOptionPane.showOptionDialog(null,
                "Выберите условие окончания игры", "Tanky 1990",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, finishOptions, finishOptions[0]);
        boolean finishByScore = finishChoice != 1;

        int targetScore = GameConfig.DEFAULT_TARGET_SCORE;
        if (finishByScore) {
            String input = JOptionPane.showInputDialog(null,
                    "Введите количество очков для победы", targetScore);
            if (input != null) {
                try {
                    targetScore = Math.max(1, Integer.parseInt(input.trim()));
                } catch (NumberFormatException ignored) {
                    targetScore = GameConfig.DEFAULT_TARGET_SCORE;
                }
            }
        }
        return new GameStartOptions(playerCount, finishByScore, targetScore);
    }

    private static class GameStartOptions {
        final int playerCount;
        final boolean finishByScore;
        final int targetScore;

        GameStartOptions(int playerCount, boolean finishByScore, int targetScore) {
            this.playerCount = playerCount;
            this.finishByScore = finishByScore;
            this.targetScore = targetScore;
        }
    }
}
