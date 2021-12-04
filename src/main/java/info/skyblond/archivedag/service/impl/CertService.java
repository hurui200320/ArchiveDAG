package info.skyblond.archivedag.service.impl;


import info.skyblond.archivedag.model.DuplicatedEntityException;
import info.skyblond.archivedag.model.EntityNotFoundException;
import info.skyblond.archivedag.model.bo.CertDetailModel;
import info.skyblond.archivedag.model.bo.CertSigningInfo;
import info.skyblond.archivedag.model.bo.CertSigningResult;
import info.skyblond.archivedag.model.entity.CertEntity;
import info.skyblond.archivedag.repo.CertRepository;
import info.skyblond.archivedag.util.GeneralKt;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

@Service
public class CertService {
    private final CertSigningInfo certSigningInfo;
    private final CertRepository certRepository;

    public CertService(CertSigningInfo certSigningInfo, CertRepository certRepository) {
        this.certSigningInfo = certSigningInfo;
        this.certRepository = certRepository;
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(this.certSigningInfo.getGeneratedKeySize(), new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    private X509Certificate signKeyPair(KeyPair userKeyPair, String subjectDN) throws IOException, CertificateException, OperatorCreationException {
        X500Name issuer = new X500Name(this.certSigningInfo.getCaCert().getIssuerDN().getName());
        X500Name subject = new X500Name(subjectDN);
        BigInteger serial;
        do {
            serial = BigInteger.valueOf(new Random().nextLong());
            serial = serial.shiftLeft(Long.BYTES * 8);
            serial = serial.or(BigInteger.valueOf(System.currentTimeMillis()));
            serial = serial.shiftLeft(Long.BYTES * 8);
            serial = serial.or(BigInteger.valueOf(System.nanoTime()));
        } while (this.certRepository.existsBySerialNumber(serial.toString(16)));

        long currentTimeStamp = System.currentTimeMillis();
        Date notBefore = new Date(currentTimeStamp);
        Date notAfter = new Date(currentTimeStamp + this.certSigningInfo.getExpireInUnit()
                .toMillis(this.certSigningInfo.getExpireInDuration()));
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(
                ASN1Sequence.getInstance(userKeyPair.getPublic().getEncoded()));

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, publicKeyInfo
        );
        AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(this.certSigningInfo.getSignAlgName());
        AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
        ContentSigner contentSigner = new BcRSAContentSignerBuilder(sigAlgId, digAlgId)
                .build(PrivateKeyFactory.createKey(this.certSigningInfo.getCaPrivateKey().getEncoded()));
        X509CertificateHolder certHolder = certBuilder.build(contentSigner);
        JcaX509CertificateConverter converter = new JcaX509CertificateConverter().setProvider("BC");
        return converter.getCertificate(certHolder);
    }

    @Transactional
    public CertSigningResult signCert(String username) throws NoSuchAlgorithmException, NoSuchProviderException, CertificateException, IOException, OperatorCreationException {
        KeyPair userKeyPair = this.generateKeyPair();
        String subjectDN = this.certSigningInfo.getCustomSubjectDN();
        if (subjectDN.isBlank()) {
            subjectDN = "CN=";
        } else {
            subjectDN += ",CN=";
        }
        X509Certificate cert = this.signKeyPair(userKeyPair, subjectDN + username);

        // add cert to database
        CertEntity entity = new CertEntity(
                cert.getSerialNumber().toString(16),
                username,
                new Timestamp(cert.getNotBefore().getTime()),
                new Timestamp(cert.getNotAfter().getTime())
        );
        // No lock, might fail since at the time this cert is generating
        // the same serial number might be taken
        if (this.certRepository.existsBySerialNumber(entity.getSerialNumber())) {
            throw new DuplicatedEntityException("Cert#" + entity.getSerialNumber());
        }
        this.certRepository.save(entity);

        return new CertSigningResult(cert, userKeyPair.getPrivate());
    }

    public List<String> listCertSerialNumber(
            boolean blurOwner, String owner, Timestamp issueStart, Timestamp issueEnd,
            Timestamp expireStart, Timestamp expireEnd, Pageable pageable
    ) {
        Page<CertEntity> queryResult;
        if (blurOwner) {
            queryResult = this.certRepository.findAllByUsernameContainingAndIssuedTimeBetweenAndExpiredTimeBetween(
                    owner, issueStart, issueEnd, expireStart, expireEnd, pageable
            );
        } else {
            queryResult = this.certRepository.findAllByUsernameAndIssuedTimeBetweenAndExpiredTimeBetween(
                    owner, issueStart, issueEnd, expireStart, expireEnd, pageable
            );
        }
        List<String> result = new LinkedList<>();
        queryResult.forEach(r -> result.add(r.getSerialNumber()));
        return result;
    }

    public boolean userOwnCert(String username, String certSerialNumber) {
        return this.certRepository.existsBySerialNumberAndUsername(certSerialNumber, username);
    }

    public CertDetailModel queryCert(String serialNumber) {
        CertEntity entity = this.certRepository.findBySerialNumber(serialNumber);
        if (entity == null) {
            return null;
        }
        return new CertDetailModel(
                entity.getSerialNumber(), entity.getUsername(),
                GeneralKt.getUnixTimestamp(entity.getIssuedTime().getTime()),
                GeneralKt.getUnixTimestamp(entity.getExpiredTime().getTime()),
                entity.getStatus()
        );
    }

    @Transactional
    public void changeCertStatus(
            String serialNumber, CertEntity.Status newStatus
    ) {
        CertEntity entity = this.certRepository.findBySerialNumber(serialNumber);
        if (entity == null) {
            throw new EntityNotFoundException("Cert#" + serialNumber);
        }
        this.certRepository.updateCertStatus(serialNumber, newStatus);
    }

    @Transactional
    public void deleteCert(String serialNumber) {
        if (!this.certRepository.existsBySerialNumber(serialNumber)) {
            throw new EntityNotFoundException("Cert#" + serialNumber);
        }
        this.certRepository.deleteBySerialNumber(serialNumber);
    }

    public void verifyCertStatus(String serialNumber, String username) {
        // check username matches the cert
        if (!this.userOwnCert(username, serialNumber)) {
            throw new BadCredentialsException("You don't own this certification.") {
            };
        }
        // get cert entity
        CertEntity entity = this.certRepository.findBySerialNumber(serialNumber);
        if (entity == null) {
            throw new BadCredentialsException("Cert#" + serialNumber + " not found") {
            };
        }
        // check cert status, must be enabled
        switch (entity.getStatus()) {
            case ENABLED:
                break;
            case DISABLED:
                throw new AuthenticationException("Certification disabled") {
                };
            case REVOKED:
                throw new AuthenticationException("Certification revoked") {
                };
            case LOCKED:
                throw new AuthenticationException("Certification locked") {
                };
        }
    }
}
