package org.jnapios.parser;

import org.jnapios.helper.FileHelper;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by pedgonca on 5/12/13.
 */
public class ParserTest {

    private File inputFile;
    private String expectedResultString;

    @Before
    public void setUp() throws IOException {
        URL inputFileUrl = ClassLoader.getSystemResource("status.dat");
        URL expectedResultFileUrl = ClassLoader.getSystemResource("result.dat");
        inputFile = new File(inputFileUrl.getFile());
        File expectedResultFile = new File(expectedResultFileUrl.getFile());
        expectedResultString = FileHelper.getFileContentAsString(expectedResultFile.getAbsolutePath(), StandardCharsets.UTF_8);
    }

    @Test
    public void testParsingFile() {
        try {
            assertEquals(expectedResultString, Parser.parser(inputFile));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception raised.");
        }
    }
}
