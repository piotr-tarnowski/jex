package com.devontrain.jex.logging;

import java.io.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 20.12.17.
 */
// ToDo [LP]: this class most probably needs renaming.
public class FileLoggingStrategy implements LoggingStrategy {

    private static final PrintStream ERR = System.err;
    private final File file;
    private final PrintStream out;

    FileLoggingStrategy(String fileName) {
        file = new File("c.d.j.Executor_" + fileName);
        ERR.println("less " + file.getAbsolutePath());
        try {
            out = new PrintStream(file);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    private void close() {
        if (file == null) return;
        if (out == null) return;
        out.close();
        try (BufferedReader in = new BufferedReader(new FileReader(file));) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        ERR.println("less " + file.getAbsolutePath());
    }

    @Override
    public void info(String message,
                     Object... args) {
        out.println(message);
    }

    @Override
    public void error(String message,
                      Object... args) {
        ERR.println(message);
    }

}
