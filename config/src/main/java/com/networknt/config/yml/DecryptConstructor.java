package com.networknt.config.yml;

import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import com.networknt.decrypt.Decryptor;
import com.networknt.decrypt.AESDecryptor;

/**
 * Decrypts values in configuration yml files.
 * 
 * @author Daniel Zhao
 *
 */
public class DecryptConstructor extends Constructor {
	private static final Decryptor decryptor = new AESDecryptor();

	public DecryptConstructor() {
		super();
		
		this.yamlConstructors.put(YmlConstants.CRYPT_TAG, new ConstructYamlDecryptedStr());
	}
	
    public static class ConstructYamlDecryptedStr extends AbstractConstruct {
        @Override
        public Object construct(Node node) {
            return constructDecryptedScalar((ScalarNode) node);
        }

		private Object constructDecryptedScalar(ScalarNode node) {
			return decryptor.decrypt(node.getValue());
		}
    }
}
