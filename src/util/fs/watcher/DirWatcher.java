package util.fs.watcher;

/*
* DEVELOPER'S NOTE:
*   BUG:
*       - Recursive change monitoring happens if output file is within the specified root directory
* */

/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


import java.io.IOException;
import java.io.PrintStream;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.FileSystems;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;


import static java.util.Calendar.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;

/**
 * Example to watch a directory (or tree) for changes to files.
 */

public class DirWatcher {
    
    // ------ Developer Note ------
//  static {
//      System.out.println("[[DirWatcher (v1.3): Note: Change the packages before final deployment]]");
//  }
    // ******* Developer Note *******
    
    private final WatchService watcher;
    private final Path dirToWatch;
    private final Map<WatchKey,Path> keys;
    private final boolean recursive;
    private boolean trace = false;
    private WatchEvent.Kind<Path>[] eventsToRegister;
    private final PrintStream out;
    private final PrintStream err;
    
    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>)event;
    }
    
    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException
    {
        WatchKey key = dir.register(watcher, eventsToRegister);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                out.format("[Register: %s] \n", dir);
            } else {
                if (!dir.equals(prev)) {
                    out.format("[Update: %s -> %s] \n", prev, dir);
                }
            }
        }
        keys.put(key, dir);
    }
    
    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException
            {
                register(dir);
//              System.out.printf("[[dirToWatch: %s, dir: %s, same: %b]] \n", dirToWatch, dir, dir.equals(dirToWatch));
                out.printf("[Registered: %s] %n",
                        (dirToWatch.equals(dir) ? "." : dirToWatch.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }
            
           @Override
           public FileVisitResult postVisitDirectory(Path dir, IOException e)
                  throws IOException
           {
                if(e != null)
                    err.printf("[Error: Failed to register directory: %s %s]",
                            dir,
                            (e.getSuppressed().length > 0) ? Arrays.deepToString(e.getSuppressed()) : "");
                return FileVisitResult.CONTINUE;
           }
           
           @Override
           public FileVisitResult visitFileFailed(Path dir, IOException e)
                  throws IOException
           {
               if(e != null)
                   err.printf("[Error: Failed to register directory: %s %s]",
                           dir,
                           (e.getSuppressed().length > 0) ? Arrays.deepToString(e.getSuppressed()) : "");
            
                return FileVisitResult.CONTINUE;
           }
        });
    }
    
    /**
     * Creates a WatchService and registers the given directory
     */
    public DirWatcher(final Path dir,
                      final boolean recursive,
                      final WatchEvent.Kind<Path>[] events,
                      final PrintStream out, final PrintStream err)
            throws IOException
    {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey,Path>();
        this.recursive = recursive;
        this.eventsToRegister = events;
        this.out = out;
        this.err = err;
        this.dirToWatch = dir;
        
        if (recursive) {
            out.printf("[Scanning '%s' ...] %n", dir);
            registerAll(dir);
            out.println("[All directories registered successfully]");
        } else {
            register(dir);
        }
        
        // enable trace after initial registration
        this.trace = true;
    }
    
    /**
     * Process all events for keys queued to the watcher
     */
    public void processEvents() {
        System.out.println("\n[ --- Watch service started --- ]\n");
        
        for (;;) {
            
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }
            
            Path dir = keys.get(key);
            if (dir == null) {
                err.println("Error: WatchKey not recognized!!");
                continue;
            }
            
            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();
                
                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    err.println("Error: Event overflow encountered!");
                }
                
                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);
                
                // print out event
                printEvent(event.kind(), dirToWatch, child);
                
                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (recursive && (kind == ENTRY_CREATE)) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (IOException x) {
                        // ignore to keep sample readable
                    }
                }
            }
            
            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);
                
                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }
    
    // cache
    private WatchEvent.Kind<?> lastEventKind;
    private Calendar lastEventTime
            = new GregorianCalendar(0,0,0,0,0,-1); // Just to force the failure of first SECOND match
    private Path lastDirToWatch, lastChild;
    private void printEvent(final WatchEvent.Kind<?> eventKind, final Path dirToWatch, final Path child) {
//      System.out.printf("[[event: %s, event kind: %s, dirToWatch: %s, child: %s]] \n",
//              event, event.kind().name(), dirToWatch, child);
        
        Calendar eventTime = Calendar.getInstance();
        if(!(  (eventTime.get(SECOND) == lastEventTime.get(SECOND)
                    && eventTime.get(MINUTE) == lastEventTime.get(MINUTE)
                        && eventTime.get(HOUR_OF_DAY) == lastEventTime.get(HOUR_OF_DAY))
            &&  eventKind.equals(lastEventKind)
            &&  dirToWatch.equals(lastDirToWatch)
            &&  child.equals(lastChild)))
        {
            out.format("%tT: %8s: %s %n", eventTime, beautifyEventName(eventKind), dirToWatch.relativize(child));
            
            // caching...
            lastEventKind = eventKind;
            lastDirToWatch = dirToWatch;
            lastChild = child;
            lastEventTime = eventTime;
        }
    }
    
    private String beautifyEventName(final WatchEvent.Kind<?> eventKind) {
        switch(eventKind.name()) {
            case "ENTRY_CREATE": return "Created";
            case "ENTRY_DELETE": return "Deleted";
            case "ENTRY_MODIFY": return "Modified";
            default: throw new AssertionError("Should not get here: Event kind not recognized! (" + eventKind.name() + ")");
        }
    }
}
