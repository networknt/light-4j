package com.networknt.config.yml;

import com.networknt.decrypt.AutoAESSaltDecryptor;
import com.networknt.decrypt.Decryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snakeyaml.engine.v2.api.ConstructNode;
import org.snakeyaml.engine.v2.constructor.StandardConstructor;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.ScalarNode;

/**
 * Decrypts values in configuration yml files.
 *
 * @author Daniel Zhao
 *
 */
public class DecryptConstructor extends StandardConstructor {
	private static final Logger logger = LoggerFactory.getLogger(DecryptConstructor.class);

	private final Decryptor decryptor;

    /** Constant for decryptor class configuration entry */
    public static final String CONFIG_ITEM_DECRYPTOR_CLASS = "decryptorClass";
    /** Constant for default decryptor class */
    public static final String DEFAULT_DECRYPTOR_CLASS = AutoAESSaltDecryptor.class.getCanonicalName();

	private DecryptConstructor() {
		this(DEFAULT_DECRYPTOR_CLASS);
	}

	private DecryptConstructor(String decryptorClass) {
		super(YamlUtils.loadSettings(true));

		decryptor= createDecryptor(decryptorClass);

		this.tagConstructors.put(YmlConstants.CRYPT_TAG, new ConstructYamlDecryptedStr());
	}

    /**
     * Creates a fresh DecryptConstructor using the default decryptor class.
     *
     * @return DecryptConstructor instance
     */
    public static DecryptConstructor create() {
        return new DecryptConstructor();
    }

    /**
     * Creates a fresh DecryptConstructor using a specific decryptor class.
     *
     * @param decryptorClass The decryptor class to use
     * @return DecryptConstructor instance
     */
    public static DecryptConstructor create(String decryptorClass) {
        return new DecryptConstructor(decryptorClass);
    }

	private Decryptor createDecryptor(String decryptorClass) {
		// do not create a new decryptor if it is already created.
		if(decryptor != null) {
			return decryptor;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("creating decryptor {}", decryptorClass);
		}

		try {
			Class<?> typeClass = Class.forName(decryptorClass);

			if (!typeClass.isInterface()) {
				return (Decryptor) typeClass.getConstructor().newInstance();
			}else {
				logger.error("Please specify an implementing class of com.networknt.decrypt.Decryptor.");
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new RuntimeException("Unable to construct the decryptor due to lack of decryption password.", e);
		}

		return null;
	}

    /**
     * Gets the decryptor used by this constructor.
     *
     * @return Decryptor instance
     */
    public Decryptor getDecryptor() {
        return decryptor;
    }

    /**
     * Inner class for constructing decrypted strings from YAML nodes.
     */
    public class ConstructYamlDecryptedStr implements ConstructNode {
        @Override
        public Object construct(Node node) {
            return constructDecryptedScalar((ScalarNode) node);
        }

		private Object constructDecryptedScalar(ScalarNode node) {
            try {
                return decryptor.decrypt(node.getValue());
            } catch (RuntimeException e) {
                String location = node.getStartMark()
                        .map(mark -> " at YAML line " + (mark.getLine() + 1) + ", column " + (mark.getColumn() + 1))
                        .orElse("");
                // Never stringify Mark/Node: those include a source snippet with the secret.
                throw new IllegalArgumentException("Unable to decrypt CRYPT configuration" + location
                        + ". Check the master password and CRYPT value format.", e);
            }
		}
    }
}
