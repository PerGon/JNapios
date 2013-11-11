package org.jnapios.tinylog;

import java.util.ArrayList;
import java.util.List;

import org.pmw.tinylog.LoggingLevel;
import org.pmw.tinylog.writers.LoggingWriter;

/**
 * Provides the ability of tinylog to log into more than one writer.
 */
public class MultiWriter implements LoggingWriter {
	private List<LoggingWriter> writers = new ArrayList<>();

	public MultiWriter() {
	}

	public void addWriter(LoggingWriter writer) {
		writers.add(writer);
	}

	@Override
	public void write(LoggingLevel level, String logEntry) {
		for (LoggingWriter writer : writers) {
			writer.write(level, logEntry);
		}
	}
}