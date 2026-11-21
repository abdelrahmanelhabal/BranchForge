package branchforge.command;

import branchforge.model.Commit;
import branchforge.model.CommitFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

class CommitPrinterTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream outContent;

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void print_formatsStandardCommitInUtcWithoutMergeLine() {

        Commit c = CommitFactory.initialCommit();
        CommitPrinter.print(c);

        SimpleDateFormat fmt = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        String ls = System.lineSeparator();

        String expected = "===" + ls
                + "commit " + c.getId() + ls
                + "Date: " + fmt.format(c.getTimeStamp()) + ls
                + ls
                + c.getMessage() + ls
                + ls;

        assertEquals(expected, outContent.toString(StandardCharsets.UTF_8));
        assertFalse(outContent.toString(StandardCharsets.UTF_8).contains("Merge:"));
    }


    @Test
    void print_throwsIllegalArgumentExceptionWhenCommitIsNull() {
        assertThrows(IllegalArgumentException.class, () -> CommitPrinter.print(null));
    }
}