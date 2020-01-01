# File System Watcher 
## Synopsis 
A utility program to watch for the registered events on the root path (can be a directory tree or a set of files).

## Features
- Option to specify the root path (can be a root directory or a set of files).
- Option to exclusively watch the path i.e. in case of directory only watches it and not any of its descendants.
- Option to specify among the following types of events to watch: creation, deletion or modification. Can also specify multiples.
- Option to specify the out file where the output of the program should be written ignoring the error messages.   

### Default behavior
- If the "root path" argument is not specified then watches the current directory tree.
- If the "exclusive" option is not enabled then watches for all possible descendants of the root path.
- If the "events" option is not specified then watches for all possible events on the root path.
- If the "out file" option is not specified then writes all the output messages to standard output stream and the error messages to standard error stream. 