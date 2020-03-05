package pl.edu.mimuw.gc401929.core;
import org.apache.tika.exception.TikaException;

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import static pl.edu.mimuw.gc401929.core.CustomIndexer.*;

import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

public class WatchDir {
	private final WatchService watcher;
	private final Map<WatchKey,Path> keys;
	private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>)event;
	}
	/**
	 * Register the given directory with the WatchService
	 */
	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
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
				return FileVisitResult.CONTINUE;
			}
		});
	}
	/**
	 * Creates a WatchService and registers the given directory
	 */
	WatchDir(ArrayList<String> paths) throws IOException {

		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey,Path>();

		for(String x: paths){
			Path dir = Paths.get(x);
			log("Scanning " + dir);
			registerAll(dir);
			log("Done, waiting for file changes");
		}
		// enable trace after initial registration
	}
	/**
	 * Process all events for keys queued to the watcher
	 */
	void processEvents() throws IOException, TikaException {
		for (;;) {
			// wait for key to be signalled
			WatchKey key;
			try {key = watcher.take();}
			catch (InterruptedException x) {return;}
			Path dir = keys.get(key);
			if (dir == null) {
				err("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event: key.pollEvents()) {
				WatchEvent.Kind kind = event.kind();
				// TBD - provide example of how OVERFLOW event is handled
				if (kind == OVERFLOW) {
					continue;
				}
				// Context for directory entry event is the file name of entry
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path child = dir.resolve(name);

				if(kind==ENTRY_CREATE) {
					log("Created: " + child);
					newFileIndex(child.toString());
				} else if (kind==ENTRY_MODIFY) {
					log("Modified: " + child);
					modifyFileIndex(child.toString());
				} else if (kind==ENTRY_DELETE) {
					log("Deleted: " + child);
					deleteFileIndex(child.toString());
				}


				// if directory is created, and watching recursively, then
				// register it and its sub-directories
				if ((kind == ENTRY_CREATE)) {
					try {if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
						registerAll(child);}}
					catch (IOException ignored) {}
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
	private static void log(String string) {
		System.out.println("[WATCHER] " + string);
	}
	private static void err(String string) {
		System.err.println("[ERROR] [WATCHER] " + string);
	}
}
