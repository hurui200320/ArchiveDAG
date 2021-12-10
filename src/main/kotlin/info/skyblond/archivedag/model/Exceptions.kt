package info.skyblond.archivedag.model

import info.skyblond.ariteg.protos.AritegLink
import info.skyblond.ariteg.util.toMultihashBase58
import io.ipfs.multihash.Multihash

class DuplicatedEntityException(entityName: String) : RuntimeException("Duplicated $entityName")
class EntityNotFoundException(entityName: String) : RuntimeException("$entityName not found")
class PermissionDeniedException(message: String) : RuntimeException(message)

class MultihashNotMatchError(
    expected: String, actual: String
) : Error(
    "Hash not match: expected: ${expected}, actual: $actual"
) {
    constructor(expected: Multihash, actual: Multihash) : this(expected.toBase58(), actual.toBase58())
}

class IllegalObjectStatusException(
    operation: String, base58Link: String, status: String
) : RuntimeException(
    "$operation cannot performed when $base58Link is $status"
) {
    constructor(operation: String, multiHash: Multihash, status: String) : this(
        operation,
        multiHash.toBase58(),
        status
    )

    constructor(operation: String, link: AritegLink, status: String) : this(
        operation,
        link.toMultihashBase58(),
        status
    )

}

/**
 * Something happened during writing the chunk.
 * Writing chunk itself won't cause any issue,
 * thus we report anything wrong to user.
 * */
class StoreProtoException(t: Throwable) : Exception("Error when storing proto", t)
class LoadProtoException(t: Throwable) : Exception("Error when loading proto", t)

class WriteChunkException(t: Throwable) : Exception("Error when writing chunk", t)
class ReadChunkException(t: Throwable) : Exception("Error when reading chunk", t)
class ReadBlobException(t: Throwable) : Exception("Error when reading blob", t)
class ReadListException(t: Throwable) : Exception("Error when reading list", t)

class WriteTreeException(t: Throwable) : Exception("Error when writing tree", t)
class ReadTreeException(t: Throwable) : Exception("Error when reading tree", t)

class WriteCommitException(t: Throwable) : Exception("Error when writing commit", t)
class ReadCommitException(t: Throwable) : Exception("Error when reading commit", t)

class ObjectRestorationException(t: Throwable) : Exception("Error when restoring object", t)
class ObjectProbingException(t: Throwable) : Exception("Error when probing object", t)
