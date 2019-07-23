package net.kebernet.xddl.plugins;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.annotation.Generated;

import static java.util.Optional.ofNullable;

public class GeneralException extends RuntimeException {
    public GeneralException(String message){
        super(message);
    }
    public GeneralException(String message, Throwable cause){
        super(message, cause);
    }

    public static <T> T wrap(Callable<T> callable){
        try {
            return callable.call();
        } catch(RuntimeException e){
            throw e;
        } catch (Exception e) {
            throw new GeneralException("Unexpected exception", e);
        }
    }

    public static void wrap(ThrowingRunnable callable){
        try {
            callable.run();
        } catch(RuntimeException e){
            throw e;
        } catch (Exception e) {
            throw new GeneralException("Unexpected exception", e);
        }
    }

    public static <T> Consumer<T> wrap(ThrowingConsumer<T> callable){
        return t -> {
            try {
                callable.run(t);
            } catch(RuntimeException e){
                throw e;
            } catch (Exception e) {
                throw new GeneralException("Unexpected exception", e);
            }
        };
    }

    public static <T> T maybeOrThrow(T value, String message){
        return ofNullable(value).orElseThrow(()->new GeneralException(message));
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void run(T value) throws Exception;
    }
}
