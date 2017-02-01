import org.encog.ml.MLMethod;
import org.encog.neural.neat.training.species.OriginalNEATSpeciation;
import org.encog.neural.networks.BasicNetwork;

import org.encog.ml.ea.train.EvolutionaryAlgorithm;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.NEATPopulation;
import org.encog.neural.neat.NEATUtil;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;


/**
 * Created by ethanm on 1/25/17.
 * Trains network using competitive unsupervised learning with NEAT
 */
public class MainTrain {

    private static TrainingData playerData;
    private static final int popSize = 500;

    public static void main(String[] args) {
        readFiles();
        //Train
        while (true) {
            System.out.println("\nEpoch: " + playerData.epoch);
            trainIteration();
            playerData.epoch++;
            writeFiles();
        }
    }

    public static void readFiles() {
        ObjectInputStream in;
        try {
            //Get player data
            in = new ObjectInputStream(new FileInputStream("training-data.td"));
            playerData = (TrainingData) in.readObject();
        } catch (IOException e) {
            //When nothing found
            playerData = new TrainingData();
            playerData.reset();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void writeFiles() {
        ObjectOutputStream out;

        try {
            //Write previous bests
            out = new ObjectOutputStream(new FileOutputStream("training-data.td"));
            out.writeObject(playerData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static NEATPopulation createPop(int size) { //Generate a template population
        int inputNeurons = 9;
        int outputNeurons = 9;
        NEATPopulation network = new NEATPopulation(inputNeurons, outputNeurons, size);
        network.reset();
        return network;
    }

    private static void trainIteration() {
        NEATPopulation pop;
        pop = createPop(popSize); //Create population

        EvolutionaryAlgorithm train; //Create training
        train = NEATUtil.constructNEATTrainer(pop, new PlayerScore(playerData.previousBests));

        OriginalNEATSpeciation speciation = new OriginalNEATSpeciation();
        train.setSpeciation(speciation);

        //Train to beat
        do {
            train.iteration();
        } while (train.getError() <= playerData.bestFitness);

        //Check if better
        System.out.println("Competitive - " + " Opponents: " + playerData.previousBests.length + " Score:" + train.getError() + " Population size: " + popSize);

        playerData.previousBests = append(playerData.previousBests, train.getCODEC().decode(pop.getBestGenome()));
        playerData.bestFitness = (int)train.getError();

        train.finishTraining();
    }

    private static void playVsHuman() {
        NEATNetwork network;
        network = (NEATNetwork) playerData.previousBests[playerData.previousBests.length-1];
        while (true) {
            TicTacToeGame humanGame = new TicTacToeGame();
            humanGame.initializeGame();
            while (humanGame.winner == -2) {
                humanGame.turnHuman(network);
            }
        }
    }

    private static MLMethod[] append(MLMethod[] oldArray, MLMethod toAppend) {
        MLMethod[] newArray = new MLMethod[oldArray.length + 1];
        for (int i = 0; i < oldArray.length; i++) {
            newArray[i] = oldArray[i];
        }
        newArray[newArray.length - 1] = toAppend;
        return newArray;
    }
}