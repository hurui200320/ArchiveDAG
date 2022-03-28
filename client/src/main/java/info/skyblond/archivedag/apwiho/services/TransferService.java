package info.skyblond.archivedag.apwiho.services;

import com.google.protobuf.ByteString;
import info.skyblond.archivedag.actohw.BlobDescriptor;
import info.skyblond.archivedag.actohw.Slicer;
import info.skyblond.archivedag.ariteg.multihash.MultihashProvider;
import info.skyblond.archivedag.ariteg.multihash.MultihashProviders;
import info.skyblond.archivedag.ariteg.protos.*;
import info.skyblond.archivedag.arudaz.protos.common.Empty;
import info.skyblond.archivedag.arudaz.protos.transfer.*;
import io.ipfs.multihash.Multihash;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class TransferService {
    private static final TransferService ourInstance = new TransferService();

    public static TransferService getInstance() {
        return ourInstance;
    }

    private TransferService() {
    }

    private final Path workDir = Path.of("./temp");
    private Multihash.Type primaryHashType;
    private MultihashProvider primaryHashProvider;
    private Multihash.Type secondaryHashType;
    private MultihashProvider secondaryHashProvider;

    // ------------------------------ gRPC calls ------------------------------

    private ProtoTransferServiceGrpc.ProtoTransferServiceFutureStub getStub() {
        return GrpcClientService.getInstance().getProtoTransferServiceFutureStub();
    }

    public void queryServerProtoConfig() throws ExecutionException, InterruptedException {
        var result = this.getStub().queryServerProtoConfig(Empty.getDefaultInstance()).get();
        this.primaryHashType = Multihash.Type.valueOf(result.getPrimaryHashType());
        this.primaryHashProvider = MultihashProviders.fromMultihashType(this.primaryHashType);
        this.secondaryHashType = Multihash.Type.valueOf(result.getSecondaryHashType());
        this.secondaryHashProvider = MultihashProviders.fromMultihashType(this.secondaryHashType);
    }

    private Slicer getSlicer(File f) {
        try {
            Files.createDirectories(this.workDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (f.length() > SlicerService.getInstance().getMinChunkSize()) {
            return SlicerService.getInstance().getDynamicSlicer(this.workDir, this.primaryHashType, this.secondaryHashType);
        } else {
            return SlicerService.getInstance().getFixedSlicer(this.workDir, this.primaryHashType, this.secondaryHashType);
        }
    }

    private AritegLink uploadBlob(String recordId, BlobDescriptor b, String name) throws IOException, ExecutionException, InterruptedException {
        var result = this.getStub().uploadBlob(UploadBlobRequest.newBuilder()
                .setRecordUuid(recordId)
                .setPrimaryHash(ByteString.copyFrom(b.primaryHash().toBytes()))
                .setBlobObj(b.readBlob())
                .build()).get();
        var receipt = result.getTransferReceipt();
        return AritegLink.newBuilder()
                .setType(AritegObjectType.BLOB)
                .setName(name)
                .setMultihash(ByteString.copyFrom(receipt, StandardCharsets.UTF_8))
                .build();
    }

    private AritegLink uploadList(String recordId, AritegListObject obj, String name) throws ExecutionException, InterruptedException {
        var result = this.getStub().uploadList(UploadListRequest.newBuilder()
                .setRecordUuid(recordId)
                .setPrimaryHash(ByteString.copyFrom(this.primaryHashProvider.digest(obj.toByteArray()).toBytes()))
                .setListObj(obj)
                .build()).get();
        var receipt = result.getTransferReceipt();
        return AritegLink.newBuilder()
                .setType(AritegObjectType.LIST)
                .setName(name)
                .setMultihash(ByteString.copyFrom(receipt, StandardCharsets.UTF_8))
                .build();
    }

    private AritegLink uploadTree(String recordId, AritegTreeObject obj, String name) throws ExecutionException, InterruptedException {
        var result = this.getStub().uploadTree(UploadTreeRequest.newBuilder()
                .setRecordUuid(recordId)
                .setPrimaryHash(ByteString.copyFrom(this.primaryHashProvider.digest(obj.toByteArray()).toBytes()))
                .setTreeObj(obj)
                .build()).get();
        var receipt = result.getTransferReceipt();
        return AritegLink.newBuilder()
                .setType(AritegObjectType.TREE)
                .setName(name)
                .setMultihash(ByteString.copyFrom(receipt, StandardCharsets.UTF_8))
                .build();
    }

    public String parseReceipt(AritegLink link) {
        return link.getMultihash().toStringUtf8();
    }

    private AritegLink proveBlobOwnership(String recordId, BlobDescriptor b, String name) {
        try {
            var result = this.getStub().proveOwnership(ProveOwnershipRequest.newBuilder()
                    .setRecordUuid(recordId)
                    .setPrimaryHash(ByteString.copyFrom(b.primaryHash().toBytes()))
                    .setSecondaryHash(ByteString.copyFrom(this.secondaryHashProvider.digest(b.readBlob().toByteArray()).toBytes()))
                    .build()).get();
            var receipt = result.getTransferReceipt();
            return AritegLink.newBuilder()
                    .setType(AritegObjectType.BLOB)
                    .setName(name)
                    .setMultihash(ByteString.copyFrom(receipt, StandardCharsets.UTF_8))
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private AritegLink sendBlob(String recordId, BlobDescriptor b, String name) throws IOException, ExecutionException, InterruptedException {
        AritegLink result = this.proveBlobOwnership(recordId, b, name);
        if (result == null) {
            result = this.uploadBlob(recordId, b, name);
        }
        return result;
    }

    public AritegBlobObject downloadBlob(String receipt) throws ExecutionException, InterruptedException {
        return this.getStub().readBlob(ReadObjectRequest.newBuilder()
                .setTransferReceipt(receipt)
                .build()).get().getBlobObj();
    }

    private AritegListObject downloadList(String receipt) throws ExecutionException, InterruptedException {
        return this.getStub().readList(ReadObjectRequest.newBuilder()
                .setTransferReceipt(receipt)
                .build()).get().getListObj();
    }

    public AritegTreeObject downloadTree(String receipt) throws ExecutionException, InterruptedException {
        return this.getStub().readTree(ReadObjectRequest.newBuilder()
                .setTransferReceipt(receipt)
                .build()).get().getTreeObj();
    }

    public AritegCommitObject downloadCommit(String receipt) throws ExecutionException, InterruptedException {
        return this.getStub().readCommit(ReadObjectRequest.newBuilder()
                .setTransferReceipt(receipt)
                .build()).get().getCommitObj();
    }

    private AritegObjectType validateObjectType(String receipt) throws ExecutionException, InterruptedException {
        return this.getStub().validateObject(ReadObjectRequest.newBuilder()
                .setTransferReceipt(receipt)
                .build()).get().getObjectType();
    }

    private Multihash validateObjectPrimaryHash(String receipt) throws ExecutionException, InterruptedException, IOException {
        return Multihash.deserialize(this.getStub().validateObject(ReadObjectRequest.newBuilder()
                .setTransferReceipt(receipt)
                .build()).get().getPrimaryHash().toByteArray());
    }

    private File multihashToFile(Multihash multihash) {
        String base58 = multihash.toBase58();
        String prefix = base58.substring(0, 6);
        File baseDir = new File(this.workDir.toFile(), prefix);
        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) {
                System.err.println("Failed to create dir: " + baseDir);
            }
        }
        return new File(baseDir, base58);
    }

    private AritegBlobObject getBlob(String receipt) throws IOException, ExecutionException, InterruptedException {
        var multihash = this.validateObjectPrimaryHash(receipt);
        var file = this.multihashToFile(multihash);
        if (file.exists()) {
            return new BlobDescriptor(multihash, file).readBlob();
        } else {
            return this.downloadBlob(receipt);
        }
    }

    private AritegLink cherryPick(String newRecordId, AritegLink link) throws ExecutionException, InterruptedException {
        var newReceipt = this.getStub().cherryPick(CherryPickRequest.newBuilder()
                .setTargetRecordUuid(newRecordId)
                .setCurrentTransferReceipt(this.parseReceipt(link))
                .build()).get().getTransferReceipt();
        return link.toBuilder().setMultihash(ByteString.copyFrom(newReceipt, StandardCharsets.UTF_8)).build();
    }

    // ------------------------------ Functions ------------------------------

    public AritegLink sliceAndUploadFile(String recordId, File f) throws ExecutionException, InterruptedException, IOException {
        System.out.println("Slicing file: " + f);
        var slicer = this.getSlicer(f);
        var linkList = new LinkedList<>(slicer.digestAsync(f)
                .parallel()
                .map(t -> {
                    BlobDescriptor b;
                    AritegLink link;
                    try {
                        b = t.get();
                        link = this.sendBlob(recordId, b, f.getName());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return link;
                }).toList());
        System.out.println("Processing file: " + f);
        // make links into ListObjects
        int i = 0;
        int listLength = 1024;
        while (linkList.size() > 1) {
            // get some link
            int listSize = Math.min(listLength, linkList.size() - i);
            List<AritegLink> list = new LinkedList<>();
            for (int j = 0; j < listSize; j++) {
                list.add(linkList.remove(i));
            }
            // make it a list
            AritegListObject listObject = AritegListObject.newBuilder()
                    .addAllLinks(list)
                    .build();
            var receipt = this.uploadList(recordId, listObject, f.getName());
            // update i (next fetch point at next iter)
            linkList.add(i++, receipt);
            // reset i if remained links are not enough for a new list
            if (i + listLength > linkList.size()) {
                i = 0;
            }
        }
        // if this is an empty file
        if (linkList.size() == 0) {
            // upload an empty blob
            linkList.add(this.sendBlob(recordId, new BlobDescriptor(
                    this.primaryHashProvider.digest(new byte[0]), f
            ), f.getName()));
        }
        return linkList.get(0);
    }

    public AritegLink sliceAndUploadFolder(String recordId, File folder) throws ExecutionException, InterruptedException {
        System.out.println("Slicing folder: " + folder);
        var files = Objects.requireNonNull(folder.listFiles(), "List folder returns null");
        var linkList = Arrays.stream(files)
                .sorted(Comparator.comparing(File::getName, Comparator.naturalOrder()))
                .map(f -> {
                    try {
                        if (f.isDirectory()) {
                            return this.sliceAndUploadFolder(recordId, f);
                        } else {
                            return this.sliceAndUploadFile(recordId, f);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toList();
        var treeObj = AritegTreeObject.newBuilder()
                .addAllLinks(linkList)
                .build();
        return this.uploadTree(recordId, treeObj, folder.getName());
    }

    public AritegLink wrapIntoTree(String recordId, AritegLink link) throws ExecutionException, InterruptedException {
        var treeObj = AritegTreeObject.newBuilder()
                .addLinks(link)
                .build();
        return this.uploadTree(recordId, treeObj, "");
    }

    public void downloadTreeIntoFolder(String receipt, File folder) throws ExecutionException, InterruptedException, IOException {
        System.out.println("Downloading into folder: " + folder);
        var treeObj = this.downloadTree(receipt);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("Failed to create folder: " + folder);
        }
        for (AritegLink link : treeObj.getLinksList()) {
            switch (link.getType()) {
                case BLOB -> this.downloadBlobIntoFile(this.parseReceipt(link), new File(folder, link.getName()));
                case LIST -> this.downloadListIntoFile(this.parseReceipt(link), new File(folder, link.getName()));
                case TREE -> this.downloadTreeIntoFolder(this.parseReceipt(link), new File(folder, link.getName()));
            }
        }
    }

    public void downloadListIntoFile(String receipt, File file) throws ExecutionException, InterruptedException, IOException {
        System.out.println("Download into file: " + file);
        FileOutputStream outputStream = new FileOutputStream(file);
        LinkedList<AritegLink> list = new LinkedList<>(this.downloadList(receipt).getLinksList());
        while (!list.isEmpty()) {
            var l = list.pollFirst();
            if (l.getType() == AritegObjectType.LIST) {
                var listObj = this.downloadList(this.parseReceipt(l));
                list.addAll(0, listObj.getLinksList());
            } else {
                var blob = this.getBlob(this.parseReceipt(l));
                outputStream.write(blob.getData().toByteArray());
            }
        }
        outputStream.close();
    }

    public void downloadBlobIntoFile(String receipt, File file) throws ExecutionException, InterruptedException, IOException {
        System.out.println("Downloading into file: " + file);
        var blob = this.getBlob(receipt);
        Files.write(file.toPath(), blob.getData().toByteArray());
    }
}
