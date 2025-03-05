package battleship.server;

import java.util.ArrayList;
import java.util.List;

public class Log {
    private final List<LogEntry> entries = new ArrayList<>();

    public void append(LogEntry entry) {
        entries.add(entry);
    }

    public LogEntry get(int index) {
        return index < entries.size() ? entries.get(index) : null;
    }

    public LogEntry getLastElem() {
        return entries.get(entries.size() - 1);
    }

    public int lastIndex() {
        return entries.size() - 1;
    }

    public int size() {
        return entries.size();
    }

    public void cleanup(){
        entries.clear();
    }
}
