package org.biojava.utils;

import java.io.*;
import java.util.*;

/**
 * @author Primary author unknown
 * @author Greg Cox
 */

public abstract class FileAsList
extends
  AbstractList
implements
  Commitable
{
  private static final int LEADER = 4;

  private RandomAccessFile mappedFile;
  private int commitedRecords;
  private int lastIndx = -1;
  private Object lastRec;
  private byte[] buffer;
  private int sizeCache = -1;

  public FileAsList(File mappedFile, int recordLength)
  throws IOException {
    if(mappedFile.exists()) {
      throw new IOException("Can't create file as it already exists: " + mappedFile);
    }
    mappedFile.createNewFile();
    this.mappedFile = new RandomAccessFile(mappedFile, "rw");
    buffer = new byte[recordLength];
    this.mappedFile.seek(0l);
    byte[] rl = String.valueOf(recordLength).getBytes();
    if(rl.length > LEADER) {
      throw new IOException("Length of record too long"); // FIXME: ugg
    }
    for(int i = 0; i < rl.length; i++) {
      this.mappedFile.write(rl[i]);
    }
    for(int i = rl.length; i < LEADER; i++) {
      this.mappedFile.write(' ');
    }

    this.mappedFile.close();
  }

  public FileAsList(File mappedFile)
  throws IOException {
    if(!mappedFile.exists()) {
      throw new IOException("Can't load mapped list as the file does not exist: " + mappedFile);
    }

    this.mappedFile = new RandomAccessFile(mappedFile, "rw");
    StringBuffer sbuff = new StringBuffer();
    this.mappedFile.seek(0l);
    for(int i = 0; i < Math.min(LEADER, mappedFile.length()); i++) {
      char c = (char) this.mappedFile.readByte();
      sbuff.append(c);
    }

    buffer = new byte[Integer.parseInt(sbuff.substring(0).trim())];
  }

  public byte[] rawGet(int indx) {
    if(indx < 0 || indx >= size()) {
      throw new IndexOutOfBoundsException("Can't access element: " + indx + " of " + size());
    }

    if(indx != lastIndx) {
      long offset = fixOffset(indx * buffer.length);
      try {
        mappedFile.seek(offset);
        mappedFile.readFully(buffer);
      } catch (IOException ioe) {
        System.out.println("Fucked up");
        throw new NestedError(ioe, "Failed to seek for record");
      }
    }

    return buffer;
  }

  public Object get(int indx) {
    if(indx == lastIndx) {
      return lastRec;
    }

    byte[] buffer = rawGet(indx);

    lastRec = parseRecord(buffer);
    lastIndx = indx;

    return lastRec;
  }

  public int size() {
    if(sizeCache < 0) {
      try {
        sizeCache = (int) (unFixOffset(mappedFile.length()) / (long) buffer.length);
      } catch (IOException ioe) {
        throw new NestedError(ioe, "Can't read file length");
      }
    };

    return sizeCache;
  }

  public boolean add(Object o) {
    sizeCache = -1;

    try {
      generateRecord(buffer, o);
    } catch (NestedException ne) {
      throw new NestedError(ne, "Failed to write index");
    }

    try {
      mappedFile.seek(mappedFile.length());
      mappedFile.write(buffer);
    } catch (IOException ioe) {
      throw new NestedError(ioe, "Failed to write index");
    }

    return true;
  }

  /**
   * This always returns null, not the previous object
   */
  public Object set(int indx, Object o) {
    try {
      generateRecord(buffer, o);
    } catch (NestedException ne) {
      throw new NestedError(ne, "Failed to write index");
    }

    try {
      mappedFile.seek(fixOffset(indx * buffer.length));
      mappedFile.write(buffer);
    } catch (IOException ioe) {
      throw new NestedError(ioe, "Failed to write index");
    }

    return null;
  }

  public void clear() {
    try {
      mappedFile.setLength(fixOffset(0));
    } catch (IOException ioe) {
      throw new NestedError(ioe, "Could not truncate list");
    }
    commitedRecords = 0;
  }

  public void commit() {
    commitedRecords = this.size();
  }

  public void rollback() {
    try {
      mappedFile.setLength(fixOffset((long) commitedRecords * (long) buffer.length));
    } catch (Throwable t) {
      throw new NestedError(
      t, "Could not roll back. " +
      "The index store will be in an inconsistent state " +
      "and should be discarded. File: " + mappedFile
      );
    }
  }

  private long fixOffset(long offset) {
    return offset + (long) LEADER;
  }

  private long unFixOffset(long offset) {
    return offset - (long) LEADER;
  }

  protected abstract Object parseRecord(byte[] buffer);
  protected abstract void generateRecord(byte[] buffer, Object item)
  throws NestedException;

  public abstract Comparator getComparator();

  public Iterator iterator() {
    return new Iterator() {
      int i = 0;

      public Object next() {
        return get(i++);
      }

      public boolean hasNext() {
        return i < size();
      }

      public void remove() {}
    };
  }
}
