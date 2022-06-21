package de.jplag;

import java.io.File;
import java.util.List;

/**
 * Common interface for all languages. Each language-front end must provide a concrete language implementation.
 */
public interface Language {

    /**
     * Suffixes for the files containing code of the language. An empty array means all suffixes are valid.
     */
    String[] suffixes();

    /**
     * Descriptive name of the language.
     */
    String getName();

    /**
     * Short name of the language used for CLI options.
     */
    String getShortName();

    /**
     * Minimum number of tokens required for a match.
     */
    int minimumTokenMatch();

    /**
     * Parses a set files in a directory.
     * @param directory is the directory where the files are located.
     * @param files are the names of the files to parse.
     * @return the list of parsed JPlag tokens.
     */
    TokenList parse(File directory, String[] files);

    /**
     * Whether errors were found during the last {@link #parse}.
     */
    boolean hasErrors();

    /**
     * Determines whether the parser provide column information.
     */
    boolean supportsColumns();

    /**
     * Determines whether JPlag should use a fixed-width font in its reports.
     */
    boolean isPreformatted();

    /**
     * Number of defined parse tree tokens in the language.
     */
    int numberOfTokens();

    /**
     * Determines whether tokens from the scanner are indexed.
     */
    default boolean usesIndex() {
        return false;
    }

    /**
     * Indicates whether the input files (code) should be used as representation in the report, or different files that form
     * a view on the input files.
     */
    default boolean useViewFiles() {
        return false;
    }

    /**
     * If the language uses representation files, this method returns the suffix used for the representation files.
     */
    default String viewFileSuffix() {
        return "";
    }

    /**
     * By default, most frontends do not care about the order of the submissions.
     * @return true, if this frontends does care about the order of submissions.
     */
    default boolean expectsSubmissionOrder() {
        return false;
    }

    /**
     * Re-orders the submissions if {@link Language#expectsSubmissionOrder()} returns true.
     * @param submissionNames are root files of the submissions, meaning a file for single file submissions or a directory
     * for multi-file submissions.
     * @return the re-ordered submission files.
     */
    default List<File> customizeSubmissionOrder(List<File> submissionNames) {
        return submissionNames;
    }

}
