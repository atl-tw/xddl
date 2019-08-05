package net.kebernet.xddl.java;

import java.io.File;
import java.io.IOException;

import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

public class JavaPlugin implements Plugin {
    @Override
    public String getName() {
        return "java";
    }

    @Override
    public String generateArtifacts(Context context, File outputDirectory) throws IOException {
        return null;
    }
}
