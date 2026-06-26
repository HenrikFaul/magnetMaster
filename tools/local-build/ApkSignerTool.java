import com.android.apksig.ApkSigner;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Minimal APK signer built on the apksig library (APK Signature Scheme v2). */
public class ApkSignerTool {
    public static void main(String[] args) throws Exception {
        // args: keystore storePass keyAlias keyPass inApk outApk minSdk
        String ksPath = args[0], storePass = args[1], alias = args[2], keyPass = args[3];
        String in = args[4], out = args[5];
        int minSdk = Integer.parseInt(args[6]);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(ksPath)) {
            ks.load(fis, storePass.toCharArray());
        }
        PrivateKey key = (PrivateKey) ks.getKey(alias, keyPass.toCharArray());
        Certificate[] chain = ks.getCertificateChain(alias);
        List<X509Certificate> certs = new ArrayList<>();
        for (Certificate c : chain) certs.add((X509Certificate) c);

        ApkSigner.SignerConfig signer =
            new ApkSigner.SignerConfig.Builder("CERT", key, certs).build();

        ApkSigner.Builder b = new ApkSigner.Builder(Collections.singletonList(signer));
        b.setInputApk(new File(in));
        b.setOutputApk(new File(out));
        b.setMinSdkVersion(minSdk);
        // v2-only: covers every API >= 24 and avoids the JDK-internal PKCS7 path
        // in this apksig release, which is incompatible with modern JDKs.
        b.setV1SigningEnabled(false);
        b.setV2SigningEnabled(true);
        b.build().sign();
        System.out.println("SIGNED -> " + out);
    }
}
