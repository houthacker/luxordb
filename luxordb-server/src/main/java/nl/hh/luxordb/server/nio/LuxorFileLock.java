package nl.hh.luxordb.server.nio;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.FileLockInterruptionException;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * A {@link Lock} that must be acquired by threads that access the database.
 * The {@link LuxorFileLock} is an interprocess lock. If the lock is exclusive, this lock prevents
 * threads from the current JVM as well as those from other processes from acquiring a conflicting lock.
 *
 * @author houthacker
 */
public class LuxorFileLock implements Lock {

  private static final long FILE_RW_LOCK_POSITION = 0L;

  private static final long FILE_RW_LOCK_BYTES = 2L;

  /**
   * Whether this lock is shared.
   */
  private final boolean shared;

  /**
   * The unique file id used to acquire a JVM memory lock.
   */
  private final Lock jvmLock;

  /**
   * The channel used to acquire an interprocess lock.
   */
  private final FileChannel channel;

  /**
   * The lock object of an acquired file lock.
   */
  private FileLock fileLock;

  LuxorFileLock(final LuxorFileId id, final FileChannel channel, final boolean shared) {
    this.shared = shared;
    this.jvmLock = shared ? id.readerLock() : id.writerLock();
    this.channel = channel;
    this.fileLock = null;
  }

  /**
   * Acquires the lock.
   * If the lock is not available at the time of calling, the current thread is suspended until the lock can be
   * acquired.
   *
   * @throws OverlappingFileLockException If an overlapping lock is already held by this JVM.
   * @throws UncheckedIOException If an I/O error occurs while acquiring the lock.
   */
  @Override
  public void lock() {
    if (isNull(this.fileLock)) {
      this.jvmLock.lock();

      try {
        this.fileLock = this.channel.lock(FILE_RW_LOCK_POSITION, FILE_RW_LOCK_BYTES, this.shared);
      } catch (OverlappingFileLockException e) {
        this.jvmLock.unlock();

        throw e;
      } catch (IOException e) {
        this.jvmLock.unlock();

        throw new UncheckedIOException(e);
      }
    }
  }

  /**
   * Acquires the lock unless the current thread is interrupted. If the lock is not available at the time of calling,
   * the current thread is suspended until the lock can be acquired.
   *
   * @throws InterruptedException If the current thread is interrupted while acquiring the lock.
   * @throws OverlappingFileLockException If an overlapping lock is already held by this JVM.
   * @throws UncheckedIOException If an I/O error occurs while acquiring the lock.
   */
  @Override
  public void lockInterruptibly() throws InterruptedException {
    if (isNull(this.fileLock)) {
      this.jvmLock.lockInterruptibly();

      try {
        this.fileLock = this.channel.lock(FILE_RW_LOCK_POSITION, FILE_RW_LOCK_BYTES, this.shared);
      } catch (OverlappingFileLockException e) {
        this.jvmLock.unlock();

        throw e;
      } catch (FileLockInterruptionException _) {
        throw new InterruptedException();
      } catch (IOException e) {
        this.jvmLock.unlock();

        throw new UncheckedIOException(e);
      }
    }
  }

  /**
   * Try to acquire the lock without blocking.
   *
   * @return {@code true} if the lock was acquired, {@code false} otherwise.
   */
  @Override
  public boolean tryLock() {
    if (isNull(this.fileLock) && this.jvmLock.tryLock()) {
      try {
        this.fileLock = this.channel.tryLock(FILE_RW_LOCK_POSITION, FILE_RW_LOCK_BYTES, this.shared);
        return true;
      } catch (IOException | OverlappingFileLockException _) {
        this.jvmLock.unlock();
      }
    }

    return false;
  }

  /**
   * Try to acquire the lock while blocking for at most {@code time} units.
   *
   * @param time the maximum time to wait for the lock
   * @param unit the time unit of the {@code time} argument.
   * @return {@code true} if the lock was acquired, {@code false} otherwise.
   * @throws InterruptedException If the current thread was interrupted while waiting to acquire the lock.
   */
  @Override
  public boolean tryLock(long time, final TimeUnit unit) throws InterruptedException {
    if (isNull(this.fileLock) && this.jvmLock.tryLock(time, unit)) {
      try {
        this.fileLock = this.channel.tryLock(FILE_RW_LOCK_POSITION, FILE_RW_LOCK_BYTES, this.shared);
        return true;
      } catch (IOException | OverlappingFileLockException _) {
        this.jvmLock.unlock();
      }
    }

    return false;
  }

  /**
   * Unlock this lock. If the lock already is unlocked, this method has no effect.
   *
   * @throws UncheckedIOException if releasing the underlying file lock causes an I/O error.
   */
  @Override
  public void unlock() {
    if (nonNull(this.fileLock)) {
      try {
        this.fileLock.release();
        this.fileLock = null;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }

      this.jvmLock.unlock();
    }
  }

  /**
   * This lock does not support conditions.
   *
   * @throws UnsupportedOperationException on every method call.
   */
  @Override
  public Condition newCondition() {
    throw new UnsupportedOperationException();
  }
}
