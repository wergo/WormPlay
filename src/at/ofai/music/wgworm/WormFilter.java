package at.ofai.music.wgworm;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * WormFilter filters .worm and .WORM files.
 *
 * @author Werner Goebl, 27 Sept 2012
 */
public class WormFilter extends FileFilter {

    /**
     * Accept all directories and all worm files.
     *
     * @param f
     * @return
     */
    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        String extension = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            extension = s.substring(i + 1).toLowerCase();
        }
        if (extension != null) {
            return extension.equals("worm") || extension.equals("WORM");
        }
        return false;
    } // accept()

    /**
     * The description of this CEUS filter
     *
     * @return
     */
    @Override
    public String getDescription() {
        return "worm files (.worm)";
    } // getDescription()

} // WormFilter
