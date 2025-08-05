package nl.hh.luxordb.server.nio;

import java.io.Closeable;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;

/**
 * A `LuxorFile` provides the basic I/O operations used by `luxordb`.
 *
 * @author houthacker
 */
public class LuxorFile implements Closeable {

  /**
   * The location at which the file resides.
   */
  private final Path path;

  /**
   * The channel to use to access the file.
   */
  private final FileChannel channel;

  /**
   * The unique id of this file.
   */
  private final LuxorFileId id;

  private LuxorFile(final Path path, final FileChannel channel) throws IOException {
    this.path = path;
    this.channel = channel;
    this.id = LuxorFileId.forPath(path);
  }

  /**
   * Create a new `LuxorFile` at the given path, assuming it does not yet exist. The file will be opened for
   * reading and writing.
   *
   * @param path The path at which the file must reside.
   * @param sparse If `true`, provides a hint that the new file will be sparse.
   * @param scoped If `true`, the file will be deleted after {@link #close()} is called.
   * @return The newly crated file.
   * @throws FileAlreadyExistsException If the file already exists when attempting to create it.
   * @throws IOException If another I/O error occurs.
   */
  public static LuxorFile create(final Path path, final boolean sparse, final boolean scoped) throws IOException {
    var options = new HashSet<OpenOption>(Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.READ,
        StandardOpenOption.WRITE));

    if (scoped) {
      options.add(StandardOpenOption.DELETE_ON_CLOSE);
    }
    if (sparse) {
      options.add(StandardOpenOption.SPARSE);
    }

    return new LuxorFile(path, FileChannel.open(path, options));
  }

  /**
   * Open a pre-existing file at the given path.
   *
   * @param path The path at which the file resides.
   * @return The opened file.
   * @throws IOException If an I/O error occurs.
   */
  public static LuxorFile open(final Path path) throws IOException {
    return new LuxorFile(path, FileChannel.open(path, Set.of(StandardOpenOption.READ,
        StandardOpenOption.WRITE)));
  }

  /**
   * Return the path of this file. May be relative.
   *
   * @return The path at which the file resides.
   */
  public Path path() {
    return this.path;
  }

  /**
   * Read a sequence of bytes from this file into the given buffer, starting at the given file position.
   *
   * @param destination The buffer into which the bytes are to be transferred.
   * @param position The file position at which the transfer is to begin. Must be non-negative.
   * @return The number of bytes read, of `-1` if the given position is greater than or equal to the file's current size.
   * @throws IOException If an I/O error occurs.
   */
  public int read(final ByteBuffer destination, long position) throws IOException {
    return this.channel.read(destination, position);
  }

  /**
   * Write a sequence of bytes to this file, starting at the given position.
   *
   * @param source The buffer from which bytes are to be transferred.
   * @param position The file position at which the transfer is to begin.
   * @return The number of bytes written.
   * @throws IOException If an I/O error occurs.
   */
  public int write(final ByteBuffer source, long position) throws IOException {
    return this.channel.write(source, position);
  }

  /**
   * Forces any updates to this file to be written to the storage device that contains it.
   *
   * @throws IOException If an I/O error occurs.
   */
  public void sync() throws IOException {
    this.channel.force(false);
  }

  /**
   * Maps the given region of this file into a `MemorySegment`. The returned segment is readable and writable.
   *
   * @param offset The offset in bytes within the file at which the segment is to start.
   * @param size The size in bytes of the mapped memory backing the segment.
   * @param arena The segment arena.
   * @return A new mapped memory segment.
   * @throws IOException If an I/O error occurs.
   */
  public MemorySegment map(final long offset, final long size, final Arena arena) throws IOException {
    return this.channel.map(FileChannel.MapMode.READ_WRITE, offset, size, arena);
  }

  /**
   * Create a new lock instance for use by reader-threads.
   *
   * @return The new lock instance.
   */
  public Lock lockShared() {
    return new LuxorFileLock(this.id, this.channel, true);
  }

  /**
   * Create a new lock instance for use by writer-threads.
   *
   * @return The new lock instance.
   */
  public Lock lockExclusive() {
    return new LuxorFileLock(this.id, this.channel, false);
  }

  /**
   * Close this file. If the file is already closed upon calling this method, this method has no effect.
   *
   * @throws IOException If an I/O error occurs.
   */
  @Override
  public void close() throws IOException {
    this.channel.close();
  }
}
