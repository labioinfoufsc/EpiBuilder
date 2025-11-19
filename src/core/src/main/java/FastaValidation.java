/**
 * Entry-point stub class for FASTA validation.
 *
 * <p>
 * This class exists in the default package to provide a simplified way of
 * invoking the {@link br.ufsc.epibuilder.FastaValidation} functionality from
 * external scripts (e.g., Nextflow pipelines) without requiring the fully
 * qualified package name.
 * </p>
 *
 * <p>
 * Internally, it delegates execution to
 * {@code br.ufsc.epibuilder.FastaValidation}, ensuring that the web
 * module can reference the class in its proper package while command-line
 * tools can continue to call it using:
 * </p>
 *
 * <pre>{@code
 * java -cp epibuilder-core.jar FastaValidation --input input.fasta ...
 * }</pre>
 *
 * <p>
 * This design maintains backward compatibility with existing scripts
 * while aligning with best practices for package organization in Java.
 * </p>
 *
 * <h2>Usage</h2>
 * <ul>
 * <li>For CLI execution: call
 * {@code java -cp epibuilder-core.jar FastaValidation}.</li>
 * <li>For backend integration: use {@link br.ufsc.epibuilder.FastaValidation}
 * directly.</li>
 * </ul>
 *
 */
public class FastaValidation {

    /**
     * Delegates execution to
     * {@link br.ufsc.epibuilder.FastaValidation#main(String[])}.
     *
     * @param args command-line arguments passed to the FASTA validation utility
     * @throws Exception if the underlying validation process fails
     */
    public static void main(String[] args) throws Exception {
        br.ufsc.epibuilder.FastaValidation.main(args);
    }
}
