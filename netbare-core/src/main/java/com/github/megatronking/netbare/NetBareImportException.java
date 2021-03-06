package com.github.megatronking.netbare;

/**
 * Indicates that an error occurred while importing a certificate, private key, or KeyStore.
 *
 * @author José Luis Ametller
 * @since 2019-02-04 15:16
 */
public class NetBareImportException extends RuntimeException {
	public NetBareImportException(String message) {
		super(message);
	}

	public NetBareImportException(String message, Throwable cause) {
		super(message, cause);
	}

	public NetBareImportException(Throwable cause) {
		super(cause);
	}
}
