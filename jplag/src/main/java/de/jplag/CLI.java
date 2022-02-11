package de.jplag;

import static de.jplag.CommandLineArgument.BASE_CODE;
import static de.jplag.CommandLineArgument.COMPARISON_MODE;
import static de.jplag.CommandLineArgument.DEBUG;
import static de.jplag.CommandLineArgument.EXCLUDE_FILE;
import static de.jplag.CommandLineArgument.LANGUAGE;
import static de.jplag.CommandLineArgument.MIN_TOKEN_MATCH;
import static de.jplag.CommandLineArgument.RESULT_FOLDER;
import static de.jplag.CommandLineArgument.ROOT_DIRECTORY;
import static de.jplag.CommandLineArgument.SIMILARITY_THRESHOLD;
import static de.jplag.CommandLineArgument.SHOWN_COMPARISONS;
import static de.jplag.CommandLineArgument.SUBDIRECTORY;
import static de.jplag.CommandLineArgument.SUFFIXES;
import static de.jplag.CommandLineArgument.VERBOSITY;
import static de.jplag.CommandLineArgument.*;

import java.io.File;
import java.util.Optional;
import java.util.Random;

import de.jplag.clustering.Algorithms;
import de.jplag.clustering.ClusteringOptions;
import de.jplag.clustering.Preprocessors;
import de.jplag.clustering.algorithm.TopDownHierarchicalClustering;
import de.jplag.exceptions.ExitException;
import de.jplag.options.JPlagOptions;
import de.jplag.options.LanguageOption;
import de.jplag.options.SimilarityMetric;
import de.jplag.options.Verbosity;
import de.jplag.reporting.Report;
import de.jplag.strategy.ComparisonMode;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Command line interface class, allows using via command line.
 * @see CLI#main(String[])
 */
public class CLI {

    private static final String CREDITS = "Created by IPD Tichy, Guido Malpohl, and others. JPlag logo designed by Sandro Koch. Currently maintained by Sebastian Hahner and Timur Saglam.";

    private static final String[] DESCRIPTIONS = {"Detecting Software Plagiarism", "Software-Archaeological Playground", "Since 1996",
            "Scientifically Published", "Maintained by SDQ", "RIP Structure and Table", "What else?", "You have been warned!", "Since Java 1.0",
            "More Abstract than Tree", "Students Nightmare", "No, changing variable names does not work", "The tech is out there!"};

    private static final String PROGRAM_NAME = "jplag";
    static final String CLUSTERING_GROUP_NAME = "Clustering";
    static final String CLUSTERING_PREPROCESSING_GROUP_NAME = "Clustering - Preprocessing";

    private final ArgumentParser parser;

    /**
     * Main class for using JPlag via the CLI.
     * @param args are the CLI arguments that will be passed to JPlag.
     */
    public static void main(String[] args) {
        try {
            CLI cli = new CLI();
            Namespace arguments = cli.parseArguments(args);
            JPlagOptions options = cli.buildOptionsFromArguments(arguments);
            JPlag program = new JPlag(options);
            System.out.println("JPlag initialized");
            JPlagResult result = program.run();
            File reportDir = new File(arguments.getString(RESULT_FOLDER.flagWithoutDash()));
            Report report = new Report(reportDir, options);
            report.writeResult(result);
        } catch (ExitException exception) {
            System.out.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }

    /**
     * Creates the command line interface and initializes the argument parser.
     */
    public CLI() {
        parser = ArgumentParsers.newFor(PROGRAM_NAME).build().defaultHelp(true).description(generateDescription());
        CliGroupHelper groupHelper = new CliGroupHelper(parser);
        for (CommandLineArgument argument : CommandLineArgument.values()) {
            argument.parseWith(parser, groupHelper);
        }
    }

    /**
     * Parses an array of argument strings.
     * @param arguments is the array to parse.
     * @return the parsed arguments in a {@link Namespace} format.
     */
    public Namespace parseArguments(String[] arguments) {
        try {
            return parser.parseArgs(arguments);
        } catch (ArgumentParserException exception) {
            parser.handleError(exception);
            System.exit(1);
        }
        return null;
    }

    /**
     * Builds a options instance from parsed arguments.
     * @param namespace encapsulates the parsed arguments in a {@link Namespace} format.
     * @return the newly built options.F
     */
    public JPlagOptions buildOptionsFromArguments(Namespace namespace) {
        String fileSuffixString = SUFFIXES.getFrom(namespace);
        String[] fileSuffixes = new String[] {};
        if (fileSuffixString != null) {
            fileSuffixes = fileSuffixString.replaceAll("\\s+", "").split(",");
        }
        LanguageOption language = LanguageOption.fromDisplayName(LANGUAGE.getFrom(namespace));
        JPlagOptions options = new JPlagOptions(ROOT_DIRECTORY.getFrom(namespace), language);
        options.setBaseCodeSubmissionName(BASE_CODE.getFrom(namespace));
        options.setVerbosity(Verbosity.fromOption(VERBOSITY.getFrom(namespace)));
        options.setDebugParser(DEBUG.getFrom(namespace));
        options.setSubdirectoryName(SUBDIRECTORY.getFrom(namespace));
        options.setFileSuffixes(fileSuffixes);
        options.setExclusionFileName(EXCLUDE_FILE.getFrom(namespace));
        options.setMinimumTokenMatch(MIN_TOKEN_MATCH.getFrom(namespace));
        options.setSimilarityThreshold(SIMILARITY_THRESHOLD.getFrom(namespace));
        options.setMaximumNumberOfComparisons(SHOWN_COMPARISONS.getFrom(namespace));
        ComparisonMode.fromName(COMPARISON_MODE.getFrom(namespace)).ifPresentOrElse(it -> options.setComparisonMode(it),
                () -> System.out.println("Unknown comparison mode, using default mode!"));

        ClusteringOptions.Builder clusteringBuilder = new ClusteringOptions.Builder();
        Optional.ofNullable((Boolean) CLUSTER_ENABLE.getFrom(namespace)).ifPresent(clusteringBuilder::enabled);
        Optional.ofNullable((Algorithms) CLUSTER_ALGORITHM.getFrom(namespace)).ifPresent(clusteringBuilder::algorithm);
        Optional.ofNullable((SimilarityMetric) CLUSTER_METRIC.getFrom(namespace)).ifPresent(clusteringBuilder::similarityMetric);
        Optional.ofNullable((Float) CLUSTER_SPECTRAL_BANDWIDTH.getFrom(namespace)).ifPresent(clusteringBuilder::spectralKernelBandwidth);
        Optional.ofNullable((Float) CLUSTER_SPECTRAL_NOISE.getFrom(namespace)).ifPresent(clusteringBuilder::spectralGPVariance);
        Optional.ofNullable((Integer) CLUSTER_SPECTRAL_MIN_RUNS.getFrom(namespace)).ifPresent(clusteringBuilder::spectralMinRuns);
        Optional.ofNullable((Integer) CLUSTER_SPECTRAL_MAX_RUNS.getFrom(namespace)).ifPresent(clusteringBuilder::spectralMaxRuns);
        Optional.ofNullable((Integer) CLUSTER_SPECTRAL_KMEANS_ITERATIONS.getFrom(namespace)).ifPresent(clusteringBuilder::spectralMaxKMeansIterationPerRun);
        Optional.ofNullable((Float) CLUSTER_AGGLOMERATIVE_THRESHOLD.getFrom(namespace)).ifPresent(clusteringBuilder::agglomerativeThreshold);
        Optional.ofNullable((TopDownHierarchicalClustering.InterClusterSimilarity) CLUSTER_AGGLOMERATIVE_INTER_CLUSTER_SIMILARITY.getFrom(namespace)).ifPresent(clusteringBuilder::agglomerativeInterClusterSimilarity);
        Optional.ofNullable((Boolean) CLUSTER_PREPROCESSING_NONE.getFrom(namespace)).ifPresent(none -> {
            if (none) {
                clusteringBuilder.preprocessor(Preprocessors.NONE);
            }
        });
        Optional.ofNullable((Boolean) CLUSTER_PREPROCESSING_CDF.getFrom(namespace)).ifPresent(cdf -> {
            if (cdf) {
                clusteringBuilder.preprocessor(Preprocessors.CDF);
            }
        });
        Optional.ofNullable((Float) CLUSTER_PREPROCESSING_PERCENTILE.getFrom(namespace)).ifPresent(percentile -> {
            clusteringBuilder.preprocessor(Preprocessors.PERCENTILE);
            clusteringBuilder.preprocessorPercentile(percentile);
        });
        Optional.ofNullable((Float) CLUSTER_PREPROCESSING_THRESHOLD.getFrom(namespace)).ifPresent(threshold -> {
            clusteringBuilder.preprocessor(Preprocessors.THRESHOLD);
            clusteringBuilder.preprocessorPercentile(threshold);
        });
        options.setClusteringOptions(clusteringBuilder.build());

        return options;
    }

    private String generateDescription() {
        var randomDescription = DESCRIPTIONS[new Random().nextInt(DESCRIPTIONS.length)];
        return String.format("JPlag - %s" + System.lineSeparator() + CREDITS, randomDescription);
    }
}
