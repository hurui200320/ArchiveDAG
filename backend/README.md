# backend

## Intro

This is the backend submodule for this project.

## Packages

There are 3 different packages for different tasks.

### ariteg

This package handle server side proto. It will handle all meta and storage management.

This package offers the following services:

+ `AritegService`: Service for proto management, load and store proto from/into the system.
    + `writeProto`: Write a proto (blob/list/tree/commit) into the system.
    + `updateMediaType`: update the media-type of a give link
    + `read{Blob,List,Tree,Commit}`: read a proto from the system.
    + `renameLink`: rename a link.
    + `deleteLink`: delete the proto and its meta data.
    + `resolveLinks`: resolve all related links of the given link.
    + `restore`: restore a list of links for read.
    + `probe`: probe a multihash and try to get the link, object type and media type.

There are multiple storage implementations located in `ariteg.storage` package. They are:

+ `LOCAL_FILE_SYSTEM_ONLY`: store protos in the local file system only.
+ `LOCAL_WITH_S3_ARCHIVE`: store protos in local, but upload to AWS S3 bucket as a passive backup.

Note: It's not recommend to use S3 only, since a big file might break into thousands of protos, using S3 will charge:

+ request fee: based on how many objects you requested
+ and transfer fee: how many bytes you downloaded from AWS

The request fee also charged when you: list protos, query proto status, upload proto. So it would be pretty expensive to
use S3 only, but S3 deep archive storage offers a great price to store cold data, start from 0.99 USD per month per 1000
GB data. So you can use your local storage for service, and manually restore from S3 when your local data is corrupted.

There is 1 table managed by this package:

+ `proto_meta`: Maintaining metadata of each proto: `primary_hash`, `secondary_hash`, `object_type`, `media_type`
  and `mark`.
    + `primary_hash`: The main key of this proto, is the primary hash of the content.
    + `secondary_hash`: The secondary hash of the content, for collision detection when writing.
    + `object_type`: Type of the proto, used for resolving link from multihash.
    + `media_type`: The media type of data which this proto refers to.
    + `mark`: Used for maintaining purpose, mark to delete, etc.

This package has it own configuration:

+ `archive-dag.ariteg`
    + `meta`: config related to meta data management
        + `lockExpireDuration`: redis lock duration, default: 5
        + `lockExpireTimeUnit`: The unit of that duration: minute
    + `storage`
        + `type`: storage type, can be: `LOCAL_FILE_SYSTEM_ONLY` or `LOCAL_WITH_S3_ARCHIVE`
        + `primaryHashType`: Multihash.Type of primary hash
        + `secondaryHashType`: Multihash.Type of secondary hash
        + `queueSize`: Queue size for writing queue
        + `threadSize`: Thread pool size for writing
        + `filesystem`: file system settings
            + `path`: data folder path
        + `s3`: AWS S3 settings
            + `region`: AWS region
            + `bucketName`: bucket name
            + `uploadStorageClass`: storage class for uploaded proto, default is `DEEP_ARCHIVE`

### arstue

This package serves all non-proto things, like user management, cert management, etc.

This package offers the following services:

+ `UserManagementService`: Service for user and their role management.
    + `listUsername`: list all username containing the keyword.
    + `userExists`: check if the given username exists.
    + `queryUser`: query the user detail.
    + `listUserRoles`: list user's roles.
    + `changePassword`: change user's password.
    + `changeStatus`: change user account's status.
    + `createUser`: create a new user with default `LOCKED` status.
    + `deleteUser`: delete user's certifications, roles and account.
    + `addRoleToUser`: append a role to the user's account.
    + `removeRoleFromUser`: remove a role from user's account.
+ `GroupService`: Service for user group management.
    + `createGroup`: create a group.
    + `deleteGroup`: remove all members and delete the group
    + `queryGroupMeta`: query the metadata of a group (owner, create time).
    + `setGroupOwner`: set group owner.
    + `addUserToGroup`: add user as the group member.
    + `removeUserFromGroup`: remove user from group.
    + `listUserOwnedGroup`: list user owned groups.
    + `listUserJoinedGroup`: list user joined groups.
    + `userIsGroupMember`: check if the user is the member of the group.
    + `userIsGroupOwner`: check if the user is the owner of the group.
    + `listGroupName`: list all group names which contains the keyword.
+ `CertService`: Service for certification management.
    + `signCert`: sign a new certification for user, default status is `LOCKED`.
    + `listCertSerialNumber`: list all certification serial numbers which satisfied the filter.
    + `userOwnCert`: check if the user own the certification.
    + `queryCert`: query the metadata of the certification (owner, status, etc.).
    + `changeCertStatus`: change the status of the certification.
    + `deleteCert`: delete the certification.
    + `verifyCertStatus`: verify the certification status, only `ENABLED` cert are accepted.
+ `FileRecordService`: Service for managing uploaded file records.
    + TODO
+ `ConfigService`: Service for managing server configurations.
    + `listConfig`: list all config having the given prefix.
    + `updateConfig`: update the config with the given key and value.
    + `allowGrpcWrite`: check if current config allow grpc write.
+ `MaintainingServier`: Service for maintaining purpose.
    + TODO
