package info.skyblond.archivedag.actohw;

import info.skyblond.archivedag.ariteg.multihash.MultihashProviders;
import info.skyblond.archivedag.ariteg.protos.AritegBlobObject;
import io.ipfs.multihash.Multihash;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public record BlobDescriptor(
        Multihash multihash,
        File file
) {
    public AritegBlobObject readBlob() throws IOException {
        try (InputStream is = new FileInputStream(this.file)) {
            MultihashProviders.mustMatch(this.multihash, is);
        }
        try (InputStream is = new FileInputStream(this.file)) {
            return AritegBlobObject.parseFrom(is);
        }
    }

}
