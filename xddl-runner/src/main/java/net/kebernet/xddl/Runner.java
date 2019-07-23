package net.kebernet.xddl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ServiceLoader;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.Builder;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

@Builder(access = AccessLevel.PUBLIC)
public class Runner {
    private File specificationFile;
    private File outputDirectory;
    private List<String> plugins;
    @Builder.Default
    private ObjectMapper mapper = new ObjectMapper();

    public static void main(String... args) {
        Command command = new Command();
        JCommander jCommander;
        try {
            jCommander = JCommander.newBuilder().addObject(command).args(args).build();
            if (command.isHelp()) {
                jCommander.usage();
                return;
            }
            Runner.builder().outputDirectory(command.getOutputDirectory())
                    .plugins(command.getFormats())
                    .specificationFile(command.getInputFile())
                    .build()
                    .run();
        } catch (Exception e) {
            e.printStackTrace();
            JCommander.newBuilder().addObject(command).build().usage();
        }
    }

    public void run() throws IOException {
        Specification spec  = this.mapper.readValue(specificationFile, Specification.class);
        Context context = new Context(mapper, spec);
        ServiceLoader<Plugin> implementations  = ServiceLoader.load(Plugin.class);
        for(Plugin plugin: implementations){
            if(plugins.contains(plugin.getName())){
                plugin.generateArtifacts(context, outputDirectory);
            }
        }
    }

}
