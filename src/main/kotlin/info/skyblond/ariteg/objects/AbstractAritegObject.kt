package info.skyblond.ariteg.objects

import info.skyblond.ariteg.AritegObject
import info.skyblond.ariteg.ObjectType

/**
 * A standard that all AritegObject should follow.
 * */
abstract class AbstractAritegObject {
    /**
     * Convert current instance to proto
     * */
    abstract fun toProto(): AritegObject

    // If bytes are same, then they are same
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractAritegObject) return false

        val thisBytes = this.toProto().toByteArray()
        val otherBytes = this.toProto().toByteArray()

        if (!thisBytes.contentEquals(otherBytes)) return false

        return true
    }

    // hash is the byte content
    override fun hashCode(): Int {
        return this.toProto().toByteArray().contentHashCode()
    }
}

/**
 * A companion object standard that all AritegObject should have.
 * */
abstract class AbstractAritegObjectCompanion<T>(
    protected val expectedType: ObjectType
) where T : AbstractAritegObject {

    /**
     * If the object is type
     * */
    fun isTypeOf(proto: AritegObject): Boolean {
        return proto.type == expectedType
    }

    /**
     * Generate instance from proto.
     * */
    fun fromProto(proto: AritegObject): T {
        require(isTypeOf(proto)) { "Expect ${expectedType.name} object but ${proto.type.name} object is given" }
        return toInstance(proto)
    }

    protected abstract fun toInstance(proto: AritegObject): T
}
