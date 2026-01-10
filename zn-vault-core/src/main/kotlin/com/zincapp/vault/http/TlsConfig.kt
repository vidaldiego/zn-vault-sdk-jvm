// Path: zn-vault-sdk-jvm/zn-vault-core/src/main/kotlin/com/zincapp/vault/http/TlsConfig.kt
package com.zincapp.vault.http

import okhttp3.OkHttpClient
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * TLS configuration for secure connections.
 */
class TlsConfig private constructor(
    private val trustManager: X509TrustManager?,
    private val sslSocketFactory: SSLSocketFactory?,
    private val hostnameVerifier: HostnameVerifier?
) {

    /**
     * Apply TLS configuration to OkHttpClient builder.
     */
    fun configureTls(builder: OkHttpClient.Builder) {
        if (sslSocketFactory != null && trustManager != null) {
            builder.sslSocketFactory(sslSocketFactory, trustManager)
        }
        if (hostnameVerifier != null) {
            builder.hostnameVerifier(hostnameVerifier)
        }
    }

    /**
     * Builder for TlsConfig.
     */
    class Builder {
        private var caCertificate: InputStream? = null
        private var clientCertificate: InputStream? = null
        private var clientKey: InputStream? = null
        private var clientKeyPassword: CharArray? = null
        private var trustAllCertificates: Boolean = false
        private var disableHostnameVerification: Boolean = false

        /**
         * Set the CA certificate for server verification.
         */
        fun caCertificate(inputStream: InputStream) = apply {
            this.caCertificate = inputStream
        }

        /**
         * Set the CA certificate from file.
         */
        fun caCertificate(file: File) = apply {
            this.caCertificate = FileInputStream(file)
        }

        /**
         * Set the CA certificate from path.
         */
        fun caCertificate(path: String) = caCertificate(File(path))

        /**
         * Set client certificate for mutual TLS (mTLS).
         */
        fun clientCertificate(inputStream: InputStream) = apply {
            this.clientCertificate = inputStream
        }

        /**
         * Set client certificate from file.
         */
        fun clientCertificate(file: File) = apply {
            this.clientCertificate = FileInputStream(file)
        }

        /**
         * Set client certificate from path.
         */
        fun clientCertificate(path: String) = clientCertificate(File(path))

        /**
         * Set client private key for mutual TLS (mTLS).
         */
        fun clientKey(inputStream: InputStream, password: CharArray? = null) = apply {
            this.clientKey = inputStream
            this.clientKeyPassword = password
        }

        /**
         * Set client private key from file.
         */
        fun clientKey(file: File, password: CharArray? = null) = apply {
            this.clientKey = FileInputStream(file)
            this.clientKeyPassword = password
        }

        /**
         * Set client private key from path.
         */
        fun clientKey(path: String, password: CharArray? = null) = clientKey(File(path), password)

        /**
         * Trust all certificates. WARNING: Use only for development!
         */
        fun trustAllCertificates(trust: Boolean = true) = apply {
            this.trustAllCertificates = trust
        }

        /**
         * Disable hostname verification. WARNING: Use only for development!
         */
        fun disableHostnameVerification(disable: Boolean = true) = apply {
            this.disableHostnameVerification = disable
        }

        /**
         * Build the TlsConfig.
         */
        fun build(): TlsConfig {
            val trustManager: X509TrustManager?
            val sslSocketFactory: SSLSocketFactory?
            val hostnameVerifier: HostnameVerifier?

            if (trustAllCertificates) {
                // Trust all certificates (insecure, for development only)
                trustManager = TrustAllTrustManager()
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, arrayOf(trustManager), java.security.SecureRandom())
                sslSocketFactory = sslContext.socketFactory
            } else if (caCertificate != null || clientCertificate != null) {
                // Build custom trust manager
                val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())

                if (caCertificate != null) {
                    val cf = CertificateFactory.getInstance("X.509")
                    val caCert = cf.generateCertificate(caCertificate) as X509Certificate

                    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                    keyStore.load(null, null)
                    keyStore.setCertificateEntry("ca", caCert)
                    tmf.init(keyStore)
                } else {
                    tmf.init(null as KeyStore?)
                }

                trustManager = tmf.trustManagers.first { it is X509TrustManager } as X509TrustManager

                // Build key manager for client certificate
                val keyManagers: Array<KeyManager>? = if (clientCertificate != null && clientKey != null) {
                    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
                    val clientKeyStore = KeyStore.getInstance("PKCS12")
                    clientKeyStore.load(clientKey, clientKeyPassword ?: charArrayOf())
                    kmf.init(clientKeyStore, clientKeyPassword ?: charArrayOf())
                    kmf.keyManagers
                } else {
                    null
                }

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(keyManagers, arrayOf(trustManager), java.security.SecureRandom())
                sslSocketFactory = sslContext.socketFactory
            } else {
                trustManager = null
                sslSocketFactory = null
            }

            hostnameVerifier = if (disableHostnameVerification) {
                HostnameVerifier { _, _ -> true }
            } else {
                null
            }

            return TlsConfig(trustManager, sslSocketFactory, hostnameVerifier)
        }
    }

    companion object {
        /**
         * Create a builder for TlsConfig.
         */
        @JvmStatic
        fun builder(): Builder = Builder()

        /**
         * Create an insecure config that trusts all certificates.
         * WARNING: Use only for development!
         */
        @JvmStatic
        fun insecure(): TlsConfig = Builder()
            .trustAllCertificates(true)
            .disableHostnameVerification(true)
            .build()
    }
}

/**
 * Trust manager that accepts all certificates.
 * WARNING: Use only for development!
 */
private class TrustAllTrustManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}
