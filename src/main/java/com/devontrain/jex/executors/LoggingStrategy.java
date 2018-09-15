package com.devontrain.jex.executors;

import java.io.*;

/**
 * Created by @author <a href="mailto:piotr.tarnowski.dev@gmail.com">Piotr Tarnowski</a> on 20.12.17.
 */
public class LoggingStrategy {
    private File file;
    private PrintStream out;
    private final PrintStream err = System.err;

    public LoggingStrategy() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    void initialzie(String name) {
        if (out != null) {
            close();
        }
        file = new File("c.d.j.Executor_" + name);
        try {
            out = new PrintStream(file);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(e);
        }
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
        System.err.println("less " + file.getAbsolutePath());
    }

    public void info(String message,
                     Object... args) {
        out.println(message);
    }

    public void error(String message,
                      Object... args) {
        err.println(message);
    }

}
