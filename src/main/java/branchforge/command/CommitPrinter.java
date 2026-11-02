package branchforge.command;

import branchforge.model.Commit;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

final class CommitPrinter {
    private CommitPrinter() {}

    static void print(Commit c) {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

        System.out.println("===");
        System.out.println("commit " + c.getId());

        if (c.isMerge()) {
            System.out.println("Merge: " + c.getParentIds().get(0).substring(0, 7)
                    + " " + c.getParentIds().get(1).substring(0, 7));
        }

        System.out.println("Date: " + fmt.format(c.getTimeStamp()));
        System.out.println();
        System.out.println(c.getMessage());
        System.out.println();
    }
}