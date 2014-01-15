package com.motorola.devicestatistics;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.util.Log;

import com.motorola.devicestatistics.DevStatUtils;

/**
 * A helper class to simplify processing of lines read from a file.<br/>
 * It ensures that files are closed correctly, and that common exceptions are caught. <br/>
 * <br/>
 * Example Usage
 * <pre>
 * class MyReader extends FileLinesReader {
 *   String result;
 *
 *   MyReader() { super("/path/to/myfile", READ_ALL_LINES); readLines(); }
 *
 *   protected abstract boolean processLine(String line) {
 *     if (line.startsWith("KEY")) { result=line; return false; }
 *     return true;
 *   }
 * }
 * </pre>
 */
abstract public class FileLinesReader {
    /**
     * The checkin tag that identifies checkin logs from this class
     */
    private static final String TAG = "FileLinesReader";
    /**
     * Constant used to specify that all lines from the file should be read.
     */
    public static final int READ_ALL_LINES = -1;
    /**
     * The file from which lines should be read
     */
    protected final String mPath;
    /**
     * The maximum number of lines to read from mPath
     */
    protected final int mMaxLines;

    /**
     * Constructs a reader that will be used to read up to maxLines from filePath.
     * @param filePath The file whose lines are to be read
     * @param maxLines The maximum number of lines to read from file. Can be READ_ALL_LINES
     */
    public FileLinesReader(String filePath, int maxLines) {
        mPath = filePath;
        if (mPath==null) Log.e(TAG,"null path: " + DevStatUtils.getStackTrace());

        mMaxLines = maxLines;
    }

    /**
     * Read and processes lines one by one, from the file.
     */
    public void readLines() {
        if (mPath==null) return;
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(mPath));
            int linesLeft = mMaxLines;
            while (linesLeft == READ_ALL_LINES || linesLeft-- > 0) {
                String line = reader.readLine();

                // line will be null, at end of file.
                if (line == null || processLine(line) == false) break;
            }
        } catch (FileNotFoundException e) {
            DevStatUtils.logException(TAG, e);
        } catch (Exception e) {
            DevStatUtils.logException(TAG, e);
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException e) {
                DevStatUtils.logException(TAG, e);
            }
        }
    }

    /**
     * Gets invoked for each line read from the file.
     * <br/>This is an abstract method that must be implemented by the subclass .<br/>
     * @param line A line read from the file
     * @return true if further lines should continue to be read. if false,
     *  no more lines will be read from the file.
     */
    protected abstract boolean processLine(String line);

    /**
     * Return the first line from the specified file
     * @param fileName - File from which to read the first line
     * @return The first line if available. Otherwise null.
     */
    public static final String getFirstLine(String fileName) {
        return new FirstLineReader(fileName).getFirstLine();
    }

    /**
     * Helper class to read the first line from a file
     *
     */
    private static final class FirstLineReader extends FileLinesReader {
        private static final int MAX_LINES_TO_READ = 1;
        private String mFirstLine;

        /**
         * @param fileName File from which to read the first line
         */
        FirstLineReader(String fileName) {
            super(fileName,MAX_LINES_TO_READ);
            readLines();
        }

        /**
         * Callback called when the first line is read
         */
        protected boolean processLine(String line) {
            mFirstLine = line;
            return false; // Read only 1st line
        }

        /**
         * @return The first line read from the file
         */
        final String getFirstLine() {
            return mFirstLine;
        }
    }
}
