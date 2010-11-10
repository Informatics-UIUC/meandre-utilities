package org.meandre.tools.components.installer.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Breadth first search over all files in a directory and it's subdirectories.
 * therefore returns all files with depth 'n' before returning those with depth
 * 'n+1'.
 * 
 * Only returns regular files: will never give a File representing a directory
 * as a result from the next() method.
 * 
 * @author Peter Groves
 */

public class FileTreeIterator implements Iterator<File> {

    /** used to navigate the directory structure */
    DirectoryTreeIterator _dirIterator;

    /**
     * contains the regular files in the directory currently being visited.
     * Files are popped off this list as next() returns them.
     */
    LinkedList<File> _currentDirContents;

    /**
     * to support hasNext() on a set of unknown size, this iterator must
     * internally store the File to be returned on the following call. This
     * holds the next value to return if it exists, or null if there are no more
     * files to return.
     */
    File _lookahead;

    /**
     * create an iterator over all files in the subdirectory of the input
     * directory treeDir.
     * 
     * @throws FileNotFoundException
     */
    public FileTreeIterator(File treeDir) throws FileNotFoundException {
        _dirIterator = new DirectoryTreeIterator(treeDir);
        _currentDirContents = new LinkedList<File>();
        _lookahead = null;

        // advance to the first dir in the tree that has at least one regular
        // file
        while ((_currentDirContents.size() == 0) && (_dirIterator.hasNext())) {
            populateDirContents(_dirIterator.next());
        }

        if (_currentDirContents.size() == 0) {
            // leaving lookahead null indicates there are no files
            return;
        }

        _lookahead = _currentDirContents.removeFirst();
    }

    public boolean hasNext() {
        return (_lookahead != null);
    }

    public File next() {
        File retFile = _lookahead;
        _lookahead = null;

        // advance to the next dir in the tree that has at least one regular
        // file
        while ((_currentDirContents.size() == 0) && (_dirIterator.hasNext())) {
            populateDirContents(_dirIterator.next());
        }

        if (_currentDirContents.size() > 0) {
            _lookahead = _currentDirContents.removeFirst();
        }
        return retFile;
    }

    /**
     * Interator interface method not supported by this class. Throws
     * UnsupportedException.
     */
    public void remove() {
        System.out.println("FileTreeIterator.remove() is not implemented");
    }

    /**
     * for the input directory, add any regular files (not directories) to this
     * class instance's _currentDirContents.
     */
    private void populateDirContents(File dirFile) {
        assert (dirFile.isDirectory());
        File[] contents = dirFile.listFiles();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i].isFile()) {
                _currentDirContents.addLast(contents[i]);
            }
        }
        return;
    }

}
