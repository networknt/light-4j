package com.networknt.config.yml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import com.networknt.decrypt.AESDecryptor;
import com.networknt.decrypt.Decryptor;

/**
 * Decrypts values in configuration yml files.
 * 
 * @author Daniel Zhao
 *
 */
public class DecryptConstructor extends Constructor {
	private static final Logger logger = LoggerFactory.getLogger(DecryptConstructor.class);
	
	private final Decryptor decryptor;
	
	public static final String CONFIG_ITEM_DECRYPTOR_CLASS = "decryptorClass";
	public static final String DEFAULT_DECRYPTOR_CLASS = AESDecryptor.class.getCanonicalName();

	public DecryptConstructor() {
		this(DEFAULT_DECRYPTOR_CLASS);
	}
	
	public DecryptConstructor(String decryptorClass) {
		super();
		
		decryptor= createDecryptor(decryptorClass);
		
		this.yamlConstructors.put(YmlConstants.CRYPT_TAG, new ConstructYamlDecryptedStr());
	}
	
	private Decryptor createDecryptor(String decryptorClass) {
		if (logger.isDebugEnabled()) {
			logger.debug("creating decryptor {}", decryptorClass);
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
	
    public class ConstructYamlDecryptedStr extends AbstractConstruct {
        @Override
        public Object construct(Node node) {
            return constructDecryptedScalar((ScalarNode) node);
        }

		private Object constructDecryptedScalar(ScalarNode node) {
			return decryptor.decrypt(node.getValue());
		}
    }
}
