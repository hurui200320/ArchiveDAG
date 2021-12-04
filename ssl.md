# SSL certs setup

## Preparation

If you're using Windows, then it's recommend to use WSL instead of OpenSSL on Windows.

Here we use OpenSSL 1.1.1 instead of the latest 3.0.

First, create a folder to hold your cert files:
```shell
mkdir ssl
cd ssl
```

## Cert for CA

First, we need a secure private key. Although ED25519 looks appealing, I still
prefer the traditional RSA. Not because RSA is good and ED25519 is bad, but just
because using RSA will be more easy to deal with (considering the compatibility
issues across different gRPC clients).

To begin with, create a private key. For this key, we want to have 4096bit length,
you can go with max of 16K bits, but considering this is a self-signed CA and you
can swap it at any time, I'll stick with the 4096 bits.

The key will NOT be encrypted since gRPC server cannot handle encryption correctly,
so just don't encrypt: 
```shell
openssl genrsa 4096 | openssl pkcs8 -topk8 -nocrypt -out rootCA.key
```

Then generate the self-signed CA:
```shell
openssl req -x509 -nodes -sha3-512 -days 7300 -key rootCA.key -out rootCA.crt -subj "/CN=SKB ArchiveDAG Root #1/O=SkyBlond"
```
Here we use `SHA3-512` as the signing algorithm and supply the subject distinguished
name as `/CN=SKB ArchiveDAG Root #1/O=SkyBlond`, which means the common name (aka `CN`)
is `SKB ArchiveDAG Root #1` and the org name is `SkyBlond`.

Since this is a self-signed certification, you can write anything.

## Cert for server

If you have a commercial SSL certification, just use that. Normally your client
will accept that, the self-signed CA is used for signing client-side certifications.

If you don't have one, you can sign your self one.

As always, generate a private key first:
```shell
openssl genrsa 4096 | openssl pkcs8 -topk8 -nocrypt -out localhost.key
```

Here we generate a 4096bit rsa private key, and encrypt it using aes256 and save
it in pkcs8 format, otherwise the gRPC server cannot parse the private key correctly.

Then generate a CSR:
```shell
openssl req -new -nodes -key localhost.key -out localhost.csr -subj "/CN=localhost"
```
Here we use `SHA3-512` again, of course you can change to `-sha3-256` or just `sha256`.
The CN here is `localhost`, which is your domain.

Then you need a file containing certificate extensions:
```shell
cat <<EOF > localhost.ext
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
subjectAltName = @alt_names
[alt_names]
DNS.1 = localhost
IP.1 = 127.0.0.1
EOF
```

You can add your domains or ips in the `[alt_names]` section. In case you don't
know, this is the `SAN` (aka the `subjectAltName`). Nowadays, almost every broswer
or ssl clients require using the `SAN`, otherwise you will get this error in gRPC
client (I'm using [evans](https://github.com/ktr0731/evans)):
```
authentication handshake failed: x509: certificate relies on legacy Common Name field, use SANs or temporarily enable Common Name matching with GODEBUG=x509ignoreCN=0
```

Finally, sign the server CSR with the key of CA:
```shell
openssl x509 -req -sha3-512 -CA rootCA.crt -CAkey rootCA.key -in localhost.csr -out localhost.crt -days 365 -CAcreateserial -extfile localhost.ext
```

This will generate the `localhost.crt`, which is the cert for our gRPC server.

## Cert for client

You can't manually issue certifications to your client, since the system track
every valid certifications by record their serial number, you can sign a legal
certification, but the serial number is unknown, it will be accepted by TLS, but
spring-security will reject it anyway.

In this system, we use `CN` to identify users, which means the `CN` is username.
That's all.

For safety reason, I recommend to issue certifications for 3 month (90 days).

When debugging, use [evans](https://github.com/ktr0731/evans) with this command
to connect to the server (assuming you have the gRPC reflect enabled, which is
super helpful when debugging):
```shell
evans -t --cacert rootCA.crt --cert client.crt --certkey client.key --host 127.0.0.1 --port 9090 -r
```
