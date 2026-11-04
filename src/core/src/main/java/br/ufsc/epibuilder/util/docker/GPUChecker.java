
package br.ufsc.epibuilder.util.docker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Checks whether CUDA is available to PyTorch inside the same BepiPred3
 * Docker image used for real inference.
 *
 */
public class GPUChecker {

    public boolean hasGpu() {
        List<String> command = List.of(
            "docker", "run", "--rm", "--gpus", "all",
            "bioinfoufsc/bepipred3",
            "python3", "-c",
            "import sys, torch; "
                + "available = torch.cuda.is_available(); "
                + "print('CUDA available to PyTorch:', available, flush=True); "
                + "sys.exit(0 if available else 1)"
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[GPUChecker] " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("[GPUChecker] CUDA test failed (exit code " + exitCode + ").");
            }
            return exitCode == 0;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[GPUChecker] CUDA test interrupted: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("[GPUChecker] Could not run Docker CUDA test: " + e.getMessage());
            return false;
        }
    }
}
