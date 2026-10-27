package crmpams.ui;

import crmpams.security.AccessDeniedException;
import java.awt.Component;
import java.awt.Cursor;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 * Runs database work off the Swing event thread so the window stays responsive,
 * then hands the result back on the event thread. Errors are logged in full but shown
 * to the user as a plain message, never as a raw SQL error.
 */
public final class UiTasks {

    private static final Logger LOG = Logger.getLogger(UiTasks.class.getName());
    private static volatile boolean synchronous;

    private UiTasks() {
    }

    /** Tests and the screenshot tool run tasks immediately on the calling thread. */
    public static void setSynchronous(boolean value) {
        synchronous = value;
    }

    public static <T> void run(Component owner, Callable<T> work, Consumer<T> onSuccess) {
        run(owner, work, onSuccess, () -> { });
    }

    /** always runs before onSuccess or the error message, for example to re-enable a button. */
    public static <T> void run(Component owner, Callable<T> work, Consumer<T> onSuccess, Runnable always) {
        if (synchronous) {
            T result;
            try {
                result = work.call();
            } catch (Exception e) {
                always.run();
                handleFailure(owner, e);
                return;
            }
            always.run();
            onSuccess.accept(result);
            return;
        }
        Cursor previous = owner.getCursor();
        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.call();
            }

            @Override
            protected void done() {
                owner.setCursor(previous);
                always.run();
                try {
                    onSuccess.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    handleFailure(owner, e.getCause());
                }
            }
        }.execute();
    }

    public static void handleFailure(Component owner, Throwable e) {
        if (e instanceof AccessDeniedException) {
            Dialogs.error(owner, "Not allowed", e.getMessage());
            return;
        }
        LOG.log(Level.SEVERE, "Operation failed", e);
        Dialogs.error(owner, "Could not complete the action",
                "The system could not finish this action. Check that the server workstation is on and "
                + "connected to the network, then try again. If the problem continues, contact the "
                + "Operations Manager.");
    }
}
