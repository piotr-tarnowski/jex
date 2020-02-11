package com.devontrain.jex.executors;

import java.io.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 20.12.17.
 */
public class AfterFileLoggingStrategy implements LoggingStrategy {
    private final File file;
    private final PrintStream out;
    private final PrintStream err = System.err;

    public AfterFileLoggingStrategy(String fileName) {
        file = new File("c.d.j.Executor_" + fileName);
        err.println("less " + file.getAbsolutePath());
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
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        err.println("less " + file.getAbsolutePath());
    }

    @Override
    public void info(String message,
                     Object... args) {
        out.println(message);
    }

    @Override
    public void error(String message,
                      Object... args) {
        err.println(message);
    }

}
