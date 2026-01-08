import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class Main {


    public static void main(String[] args) throws InterruptedException {
        //Setup
        final GameConfiguration gameConfig = new GameConfiguration(7,2,100,2000000);

        int threads = Runtime.getRuntime().availableProcessors();

        final AtomicLong runs = new AtomicLong(gameConfig.runs);
        final long staticRuns = runs.get();

        long startTime = System.currentTimeMillis();
        final AtomicLong totalWins = new AtomicLong(0);
        final AtomicLong totalLooses = new AtomicLong(0);
        final AtomicLong mainBalance = new AtomicLong(0);
        final AtomicLong gameRuns = new AtomicLong(0);
        final DecimalFormat format = new DecimalFormat("###,###");


        new Thread(() -> {
            while (true) {
                long runsL = runs.get();
                long runsDone = staticRuns - runsL;
                double percent = (runsDone * 100.0) / staticRuns;
                System.out.println("Progress: " + format.format(percent) + "%");
                if(runsL <= 0)
                    break;
            }
        }).start();

        List<Thread> allThreads = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                allThreads.add(new Thread(() -> {
                    List <GameResult> results = new ArrayList<>();
                while (true) {
                    synchronized (runs) {
                        if(runs.getAndDecrement() <= 0)
                            break;
                    }

                    results.add(runSimulation(gameConfig.neededBalance,gameConfig.goal, gameConfig.startingBet));
                }

                GameResult endGameResult = new GameResult(results.stream().mapToInt(GameResult::wins).sum(),
                        results.stream().mapToInt(GameResult::losses).sum(),
                        results.stream().mapToInt(GameResult::gameRuns).sum(),
                        results.stream().mapToInt(GameResult::balance).sum());

                    totalWins.getAndAdd(endGameResult.wins());
                    totalLooses.getAndAdd(endGameResult.losses());
                    mainBalance.getAndAdd(endGameResult.balance());
                    gameRuns.getAndAdd(endGameResult.gameRuns());
                }));
            }
        allThreads.forEach(Thread::start);



        while (allThreads.stream().anyMatch(Thread::isAlive)) {
            Thread.sleep(100);
        }

        System.out.println("===============================");
        System.out.println("Total Wins: " + format.format(totalWins.get()));
        System.out.println("Total Looses: " + format.format(totalLooses.get()));
        System.out.println("Total Balance: " + format.format(mainBalance.get()));
        System.out.println("Total Games Played: " + format.format(gameRuns.get()));
        System.out.println("Total Start Balance: " + format.format(staticRuns * gameConfig.neededBalance));
        System.out.println("Balance Lost/Won: " + format.format(mainBalance.get() - (gameConfig.neededBalance * staticRuns)));
        System.out.println("Win Rate per Game: " + format.format((totalWins.get() * 100.0) / (totalWins.get() + totalLooses.get())) + "%");
        System.out.println("Time taken: " + format.format((System.currentTimeMillis() - startTime)) + " ms");
        System.out.println("===============================");

    }

    private static GameResult runSimulation(int balance,int goal, int bet) {
        int wins = 0;
        int looses = 0;
        int games = 0;

        while (true) {

            if(bet > balance) {
                looses++;
                break;
            }

            if(goal <= balance) {
                wins++;
                break;
            }

            Random random = new SecureRandom();
            double c = random.nextDouble(99);

            if (c < 48.96) {
                balance += bet;
                bet = 2;
            } else {
                balance -= bet;
                bet *= 2;
            }
            games++;
        }

        return new GameResult(wins,looses, games, balance);
    }
}
record GameResult(int wins, int losses, int gameRuns, int balance) {

}
class GameConfiguration {
    final int goal;
    final int neededBalance;
    final int runs;
    final int startingBet;

    public GameConfiguration(int tries,int startingBet, int goal, int runs) {
        this.startingBet = startingBet;
        this.neededBalance = getNeededBalanceFromTries(tries, startingBet);
        this.goal = neededBalance+goal;
        this.runs = runs;
    }

    private int getNeededBalanceFromTries(int tries, int startingBet) {
        int balance = 0;
        for (int i = 0; i < tries; i++) {
            balance += startingBet;
            startingBet *= 2;
        }
        return balance;
    }

}