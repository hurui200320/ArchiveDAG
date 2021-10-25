package info.skyblond.archivedag.controller;

import info.skyblond.archivedag.config.ProtoProperties;
import info.skyblond.archivedag.model.ao.*;
import info.skyblond.archivedag.model.bo.ProbeReceipt;
import info.skyblond.archivedag.model.bo.ReadReceipt;
import info.skyblond.archivedag.model.bo.RestoreReceipt;
import info.skyblond.archivedag.model.bo.WriteReceipt;
import info.skyblond.archivedag.model.exception.*;
import info.skyblond.archivedag.service.intf.ProtoService;
import info.skyblond.ariteg.AritegLink;
import info.skyblond.ariteg.ObjectType;
import info.skyblond.ariteg.objects.CommitObject;
import info.skyblond.ariteg.objects.TreeObject;
import io.ipfs.multihash.Multihash;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * /proto
 * + /chunk
 * | + Post: Upload chunk of data and get links to blob or list
 * | + Get:  Download chunk of data from a link to blob or list
 * |
 * + /tree
 * | + Post: Create a tree obj from list of links
 * | + Get:  Get the list of links from a link to a tree obj
 * |
 * + /commit
 * | + Post: Create a commit obj from the given info
 * | + Get:  Display the info of a link to commit obj
 * |
 * + /restore
 * | + Post: Restore and return all involved blob object links from a list of links
 * |
 * + /status
 * | + Get:  Query the info of given links
 */

@RestController
@RequestMapping("/proto")
public class ProtoController {
    private final ProtoService protoService;
    private final ProtoProperties protoProperties;

    public ProtoController(ProtoService protoService, ProtoProperties protoProperties) {
        this.protoService = protoService;
        this.protoProperties = protoProperties;
    }

    @GetMapping(path = "/chunk")
    public ResponseEntity<Resource> downloadChunk(
            @RequestParam("link") String hashBase58
    ) throws ObjectProbingException, ReadChunkException {
        Multihash multihash = Multihash.fromBase58(hashBase58);
        ProbeReceipt probeReceipt = this.protoService.probe(multihash);
        if (probeReceipt == null) {
            return ResponseEntity.notFound().build();
        }
        AritegLink link = probeReceipt.getLink();
        ReadReceipt readReceipt = this.protoService.readChunk(link);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(readReceipt.getMediaType()))
                .body(new InputStreamResource(readReceipt.getInputStream())
                );
    }

    @PostMapping(path = "/chunk")
    public List<AritegLinkModel> uploadChunks(
            @RequestParam("files") MultipartFile[] files
    ) throws IOException, WriteChunkException, ExecutionException, InterruptedException {
        List<AritegLinkModel> result = new LinkedList<>();
        List<CompletableFuture<Void>> futures = new LinkedList<>();
        for (MultipartFile file : files) {
            try (InputStream in = file.getInputStream()) {
                WriteReceipt receipt = this.protoService.writeChunk(
                        file.getOriginalFilename(), file.getContentType(), in,
                        this.protoProperties.getBlobSize(), this.protoProperties.getListLength()
                );
                result.add(AritegLinkModel.fromAritegLink(receipt.getLink()));
                futures.add(receipt.getCompletionFuture());
            }
        }
        for (CompletableFuture<Void> future : futures) {
            future.get();
        }
        return result;
    }

    @PostMapping(path = "/tree")
    public AritegLinkModel createTree(
            @RequestBody CreateTreeRequest request
    ) throws WriteTreeException, ExecutionException, InterruptedException {
        WriteReceipt writeReceipt = this.protoService.writeTree(request.getName(), request.getAritegLinks());
        writeReceipt.getCompletionFuture().get();
        return AritegLinkModel.fromAritegLink(writeReceipt.getLink());
    }

    @GetMapping(path = "/tree")
    public List<AritegLinkModel> getTree(
            @RequestParam("link") String hashBase58
    ) throws ObjectProbingException, ReadTreeException {
        Multihash multihash = Multihash.fromBase58(hashBase58);
        ProbeReceipt probeReceipt = this.protoService.probe(multihash);
        if (probeReceipt == null) {
            return List.of();
        }
        AritegLink link = probeReceipt.getLink();
        if (link.getType() != ObjectType.TREE) {
            return List.of();
        }
        TreeObject obj = this.protoService.readTree(link);
        List<AritegLinkModel> result = new LinkedList<>();
        for (AritegLink l : obj.getLinks()) {
            result.add(AritegLinkModel.fromAritegLink(l));
        }
        return result;
    }

    @PostMapping(path = "/commit")
    public AritegLinkModel createCommit(
            @RequestBody CreateCommitRequest request
    ) throws ExecutionException, InterruptedException, WriteCommitException {
        WriteReceipt writeReceipt = this.protoService.writeCommit(
                request.getName(), request.getUnixTimestamp(), request.getMessage(),
                request.getParentAritegLink(),
                request.getCommittedObjectAritegLink(),
                request.getAuthorAritegLink()
        );
        writeReceipt.getCompletionFuture().get();
        return AritegLinkModel.fromAritegLink(writeReceipt.getLink());
    }

    @GetMapping(path = "/commit")
    public CommitObjectModel getCommit(
            @RequestParam("link") String hashBase58
    ) throws ObjectProbingException, ReadCommitException {
        Multihash multihash = Multihash.fromBase58(hashBase58);
        ProbeReceipt probeReceipt = this.protoService.probe(multihash);
        if (probeReceipt == null) {
            return null;
        }
        AritegLink link = probeReceipt.getLink();
        if (link.getType() != ObjectType.COMMIT) {
            return null;
        }
        CommitObject obj = this.protoService.readCommit(link);
        return CommitObjectModel.fromCommitObject(obj);
    }

    @PostMapping(path = "/restore")
    public List<AritegLinkModel> restore(
            @RequestBody RestoreRequest request
    ) throws ObjectProbingException, ObjectRestorationException, ExecutionException, InterruptedException {
        List<AritegLinkModel> resultLinkList = new LinkedList<>();
        List<CompletableFuture<Void>> futureList = new LinkedList<>();

        for (String hashBase58 : request.getLinks()) {
            Multihash multihash = Multihash.fromBase58(hashBase58);
            ProbeReceipt probeReceipt = this.protoService.probe(multihash);
            if (probeReceipt == null) {
                return null;
            }
            RestoreReceipt receipt = this.protoService.restore(
                    probeReceipt.getLink(), request.toRestoreOption()
            );
            for (AritegLink l : receipt.getInvolvedLinks()) {
                resultLinkList.add(AritegLinkModel.fromAritegLink(l));
            }
            futureList.add(receipt.getCompletionFuture());
        }
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).get();
        return resultLinkList;
    }

    @GetMapping(path = "/status")
    public Map<String, StatusModel> restore(
            @RequestParam("links") List<String> multihashBase58List
    ) throws ObjectProbingException, ObjectRestorationException, ExecutionException, InterruptedException {
        Map<String, StatusModel> result = new HashMap<>();

        for (String rawHashBase58 : multihashBase58List) {
            for (String hashBase58 : rawHashBase58.split(",")) {

                Multihash multihash = Multihash.fromBase58(hashBase58);
                ProbeReceipt probeReceipt = this.protoService.probe(multihash);
                if (probeReceipt == null) {
                    continue;
                }
                result.put(hashBase58, new StatusModel(
                        probeReceipt.getLink().getType().name(),
                        probeReceipt.getMediaType(),
                        probeReceipt.getStatus().getName(),
                        probeReceipt.getStatus().getObjSize()
                ));
            }
        }
        return result;
    }
}
