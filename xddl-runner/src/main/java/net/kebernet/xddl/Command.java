package net.kebernet.xddl;

import java.io.File;
import java.util.List;

import com.beust.jcommander.Parameter;
import lombok.Data;

@Data
public class Command {

    @Parameter(names = {"--input-file", "-i"},
            description = "The directory to output generated artifacts to.",
            required = true)
    private File inputFile;
    @Parameter(names = {"--output-directory", "-o"},
            description = "The directory to output generated artifacts to.",
            required = true)
    private File outputDirectory;
    @Parameter(names = {"--format", "-f"},
            description = "The output plugin to generate",
            required = true)
    private List<String> formats;
    @Parameter(names = "--help",
            description = "Show this help text",
            help = true)
    private boolean help = false;
}
