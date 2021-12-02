package info.skyblond.archivedag.model

class DuplicatedEntityException(entityName: String) : RuntimeException("Duplicated $entityName")
class EntityNotFoundException(entityName: String) : RuntimeException("$entityName not found")
class PermissionDeniedException(message: String) : RuntimeException(message)
