package net.kebernet.xddl.plugins;

import java.io.File;
import java.io.IOException;

public interface Plugin {
    String getName();
    String generateArtifacts(Context context, File outputDirectory) throws IOException;
}
