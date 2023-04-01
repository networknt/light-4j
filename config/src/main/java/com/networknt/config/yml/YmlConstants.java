package com.networknt.config.yml;

import java.util.regex.Pattern;

import org.yaml.snakeyaml.nodes.Tag;

public class YmlConstants {
	public static final Tag CRYPT_TAG = new Tag(Tag.PREFIX + "crypt");
	public static final Pattern CRYPT_PATTERN = Pattern.compile("CRYPT:[a-f0-9]+:[a-f0-9]+");
	public static final String CRYPT_FIRST = "C"; 
}
