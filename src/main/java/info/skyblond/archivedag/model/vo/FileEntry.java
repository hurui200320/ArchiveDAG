package info.skyblond.archivedag.model.vo;

import lombok.Value;

/**
 * File Entry model for web view.
 */
@Value
public class FileEntry {
    String tag;
    String multihashBase58;
    String storageClass;
}
