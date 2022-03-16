package info.skyblond.archivedag.apwiho.services;

import info.skyblond.archivedag.arudaz.protos.group.GroupServiceGrpc;
import info.skyblond.archivedag.arudaz.protos.info.UserInfoServiceGrpc;
import info.skyblond.archivedag.arudaz.protos.record.FileRecordServiceGrpc;
import info.skyblond.archivedag.arudaz.protos.transfer.ProtoTransferServiceGrpc;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class GrpcClientService {
    private static final GrpcClientService ourInstance = new GrpcClientService();

    public static GrpcClientService getInstance() {
        return ourInstance;
    }

    private GrpcClientService() {
    }

    private volatile ManagedChannel managedChannel = null;
    private volatile UserInfoServiceGrpc.UserInfoServiceFutureStub userInfoServiceFutureStub = null;
    private volatile GroupServiceGrpc.GroupServiceFutureStub groupServiceFutureStub = null;
    private volatile FileRecordServiceGrpc.FileRecordServiceFutureStub fileRecordServiceFutureStub = null;
    private volatile ProtoTransferServiceGrpc.ProtoTransferServiceFutureStub protoTransferServiceFutureStub = null;

    public void init(
            File serverCAFile, File userCertFile, File userPrivateKeyFile, String hostStr
    ) throws IOException {
        if (managedChannel == null) {
            var channelCredentialsBuilder = TlsChannelCredentials.newBuilder();
            if (serverCAFile != null) {
                channelCredentialsBuilder.trustManager(serverCAFile);
            }
            channelCredentialsBuilder.keyManager(userCertFile, userPrivateKeyFile);
            String[] t = hostStr.split(":");
            String host = t[0];
            int port = 9090;
            if (t.length > 1) {
                try {
                    port = Integer.parseInt(t[1]);
                } catch (Throwable ignored) {
                }
            }

            managedChannel = Grpc.newChannelBuilderForAddress(
                    host, port, channelCredentialsBuilder.build()
            ).build();
        }

        if (userInfoServiceFutureStub == null) {
            userInfoServiceFutureStub = UserInfoServiceGrpc.newFutureStub(managedChannel);
        }
        if (groupServiceFutureStub == null) {
            groupServiceFutureStub = GroupServiceGrpc.newFutureStub(managedChannel);
        }
        if (fileRecordServiceFutureStub == null) {
            fileRecordServiceFutureStub = FileRecordServiceGrpc.newFutureStub(managedChannel);
        }
        if (protoTransferServiceFutureStub == null) {
            protoTransferServiceFutureStub = ProtoTransferServiceGrpc.newFutureStub(managedChannel);
        }
    }

    public UserInfoServiceGrpc.UserInfoServiceFutureStub getUserInfoServiceFutureStub() {
        return Objects.requireNonNull(userInfoServiceFutureStub, "Uninitialized use");
    }

    public GroupServiceGrpc.GroupServiceFutureStub getGroupServiceFutureStub() {
        return Objects.requireNonNull(groupServiceFutureStub, "Uninitialized use");
    }

    public FileRecordServiceGrpc.FileRecordServiceFutureStub getFileRecordServiceFutureStub() {
        return Objects.requireNonNull(fileRecordServiceFutureStub, "Uninitialized use");
    }

    public ProtoTransferServiceGrpc.ProtoTransferServiceFutureStub getProtoTransferServiceFutureStub() {
        return Objects.requireNonNull(protoTransferServiceFutureStub, "Uninitialized use");
    }
}
