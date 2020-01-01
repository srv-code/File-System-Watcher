import util.StandardExitCodes;
import util.fs.watcher.DirWatcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.WatchEvent;
import static java.nio.file.LinkOption.*;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.FileNotFoundException;


public class FSWatcher
{
	// ------ Developer Note ------
//	static {
//		System.out.println("[[FSWatcher: Note: Change the packages before final deployment]]");
//	}
	// ******* Developer Note *******
	
	private static final float APP_VERSION = 1.01f;
	
	private static boolean exclusiveMode = false; // default value set
	private static Path pathToWatch = Paths.get( System.getProperty("user.dir") ); // default value set
	
	@SuppressWarnings("unchecked")
	private static WatchEvent.Kind<Path>[] eventsToRegister = new WatchEvent.Kind[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY }; // default value set
	
	private static PrintStream out = System.out; // default value set
	private static PrintStream err = System.err; // defailt value set
	
	
	public static void main(final String[] args)
	{
		try
		{
			getOptions(args);
//			$showOptions(); // DEBUG: For dev diagnosis only
			
			System.out.println("[Press Ctrl-C for the service to stop]");
			new DirWatcher(pathToWatch, (!exclusiveMode), eventsToRegister, out, err).processEvents();
		} catch(IllegalArgumentException e) {
			System.err.println("Error: Invalid argument: " + e.getMessage());
			System.exit(StandardExitCodes.ERROR);
		} catch(IOException e) {
			System.err.println("I/O Error: " + e.getMessage());
			System.exit(StandardExitCodes.FILE);
		} catch(AssertionError e) {
			System.err.println("Should not get here: " + e.getMessage());
			System.exit(StandardExitCodes.FATAL);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static void getOptions(final String[] args)
			throws 	IllegalArgumentException,
			IOException,
			AssertionError
	{
		char requireArgForOption = 0;
		
		for(String arg : args)
		{
			if(requireArgForOption != 0)
			{
				switch(requireArgForOption)
				{
					case 'p':
						// Sets path as root for watching
						pathToWatch = Paths.get(arg);
						if(!Files.exists(pathToWatch, NOFOLLOW_LINKS))
							throw new FileNotFoundException(arg + " (Cannot find the path specified)");
						
						break;
					
					
					case 'e':
						// [<event1>[,<event2>...]] : Registers individual events for watch [registers all events if not specified]
						// Events:
						// 		c : Entry create
						// 		d : Entry delete
						// 		m : Entry modify
						int create = 0, delete = 0, modify = 0;
						for(String e : arg.split(","))
						{
							switch(e)
							{
								case "c": create = 1; break;
								case "d": delete = 1; break;
								case "m": modify = 1; break;
								default: throw new IllegalArgumentException(e + " (Invalid event)");
							}
						}
						
						eventsToRegister = new WatchEvent.Kind[ create + delete + modify ];
						int i = 0;
						if(create == 1) eventsToRegister[i++] = ENTRY_CREATE;
						if(delete == 1) eventsToRegister[i++] = ENTRY_DELETE;
						if(modify == 1) eventsToRegister[i++] = ENTRY_MODIFY;
						
						break;
					
					
					case 'o':
						// [<file_path>] : Writes output to specified file
						out = new PrintStream(arg);
						
						// Disables err stream: Assigns to a new OutputStream that does absolutely nothing
						err = new PrintStream(new OutputStream() {
							@Override
							public void write(int b) throws IOException {}
						});
						
						break;
					
					
					default:
						throw new AssertionError("Invalid value set for requireArgForOption=" + requireArgForOption);
				}
				
				
				requireArgForOption = 0; // Resets after each use
			}
			else
			{
				switch(arg)
				{
					case "-x":
					case "--excl" :
					// Only watches the path and not its decendants [Default behaviour: Watches decendants also]
					exclusiveMode = true;
					break;
					
					
					case "-p":
					case "--path" :
					// Sets path as root for watching
					requireArgForOption = 'p';
					break;
					
					
					case "-e":
					case "--events":
					// [<event1>[,<event2>...]] : Registers individual events for watch [registers all events if not specified]
					requireArgForOption = 'e';
					break;
					
					
					case "-o":
					case "--ofile":
					// [<file_path>] : Writes output to specified file
					requireArgForOption = 'o';
					break;
					
					
					case "-h":
					case "--help":
					// to do
					// Shows this help menu and exists
					showHelp();
					System.exit(StandardExitCodes.NORMAL);
					
					default:
						throw new IllegalArgumentException(arg + " (Invalid option)");
				}
			}
		}
		
		if(requireArgForOption != 0)
			throw new IllegalArgumentException(requireArgForOption + " (Missing option argument)");
	}
	
//	// For developer diagnosis only
//	private static void $showOptions()
//	{
//		System.out.println("[[Disable FSWatcher.showOptions() before final deployment]]"); // DEBUG
//
//		System.out.println("-------------------- :: Option diagnosis :: --------------------");
//		System.out.println("exclusiveMode=" + exclusiveMode);
//		System.out.println("pathToWatch=" + pathToWatch);
//		System.out.println("eventsToRegister=" + java.util.Arrays.toString(eventsToRegister));
//		System.out.println("out=" + out.getClass().getName());
//		System.out.println("err=" + err.getClass().getName());
//		System.out.println("********************* :: Option diagnosis ::  *********************");
//	}
	
	private static void showHelp()
	{
		System.out.println("FSWatcher");
		System.out.printf ("Version: %.2f %n", APP_VERSION);
		System.out.printf ("\nUsage:  %s [-<option>] [<root_path>]\n", FSWatcher.class.getName());
		
		System.out.println("\nPurpose: Watches the specified directory / file for registered changes.");
		System.out.println("  Default behaviour: ");
		System.out.println("    - Watches all possible decendants.");
		System.out.println("    - Watches all possible events.");
		
		System.out.println("\nOptions: ");
		System.out.println("  --excl,   -x    Only watches the path and not its decendants [Watches decendants also by default].");
		System.out.println("  --path,   -p    Sets path as root for watching [Sets current path as root by default].");
		System.out.println("  --events, -e    [<event1>[,<event2>...]] : Registers individual events for watch [Registers all events by default].");
		System.out.println("  --ofile,  -o    [<file_path>] : Writes output to specified file and ignores errors [Writes output to STDOUT and errors to STDERR by default].");
		System.out.println("  --help,   -h    Shows this help menu and exists.");
		
		System.out.println("\nEvents:");
		System.out.println("  c    Entry create");
		System.out.println("  d    Entry delete");
		System.out.println("  m    Entry modify");
		
		StandardExitCodes.showMessage();
	}
}