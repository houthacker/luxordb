package nl.hh.luxordb.server.nio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A filesystem-wide, unique identifier of a `LuxorFile`. Any JVM instance contains at most one `LuxorFileId` per unique file.
 * Therefore, that instance can be used to get a JVM-wide lock on a file.
 *
 * @author houthacker
 */
public class LuxorFileId {

  /**
   * All `LuxorFileId` instances, mapped by their absolute paths.
   */
  private static final Map<Path, LuxorFileId> ALL = new HashMap<>();

  /**
   * Lock object for global file id map.
   */
  private static final ReentrantReadWriteLock ALL_LOCK = new ReentrantReadWriteLock();

  /**
   * The name of the file attribute containing the unique file key.
   */
  private static final String ATTR_FILE_KEY = "fileKey";


  /**
   * The unique file key.
   */
  private final Object key;

  /**
   * A lock to be obtained by threads that want to write to the database.
   */
  private final Lock writerLock;

  /**
   * A lock to be obtained by threads that want to read from the database.
   */
  private final ReentrantReadWriteLock readerLock;

  private LuxorFileId(final Object key) {
    this.key = key;
    this.writerLock = new ReentrantLock();
    this.readerLock = new ReentrantReadWriteLock();
  }

  /**
   * Retrieve the `LuxorFileId` instance of the file at the given path.
   *
   * @param path The path to query. May be relative.
   * @return The retrieved file id.
   * @throws IOException If an I/O error occurs.
   */
  public static LuxorFileId forPath(final Path path) throws IOException {
    var realPath = path.toRealPath();
    var key = Files.getAttribute(realPath, ATTR_FILE_KEY);

    ALL_LOCK.writeLock().lock();
    try {
      return ALL.computeIfAbsent(realPath, ignored -> new LuxorFileId(key));
    } finally {
      ALL_LOCK.writeLock().unlock();
    }
  }

  /**
   * Return the unique file key. This key is unique within the file system containing the file.
   *
   * @return The unique file key.
   */
  public Object key() {
    return this.key;
  }

  /**
   * Return the lock to be obtained by writing threads.
   *
   * @return The writer lock.
   */
  public Lock writerLock() {
    return this.writerLock;
  }

  /**
   * Return the lock to be obtained by reading threads.
   *
   * @return The reader lock.
   */
  public Lock readerLock() {
    return this.readerLock.readLock();
  }
}
