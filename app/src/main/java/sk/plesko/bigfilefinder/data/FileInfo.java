package sk.plesko.bigfilefinder.data;

import java.io.File;

/**
 * Created by Ivan on 22. 2. 2015.
 */
public final class FileInfo implements Comparable<FileInfo> {

    private String name;
    private long size;

    public FileInfo(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public FileInfo (File file) {
        this.name = file.getAbsolutePath();
        this.size = file.length();
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    // taken from http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java#answer-3758880
    public String getSizeFormatted() {
        int unit = 1000;
        if (size < unit) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(unit));
        char pre = "kMGTPE".charAt(exp-1);
        return String.format("%.1f %sB", size / Math.pow(unit, exp), pre);
    }

    @Override
    public int compareTo(FileInfo another) {
        // the following line is nicer, but it works from API level 19, we want this app to work on API level 15
        // return Long.compare(size, another.getSize());

        if (size < another.getSize()) {
            return 1;
        } else if (size > another.getSize()) {
            return -1;
        } else {
            return 0;
        }
    }
}
