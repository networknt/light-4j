package com.networknt.config.yml;

import java.util.regex.Pattern;

import org.yaml.snakeyaml.nodes.Tag;

public class YmlConstants {
	public static final Tag CRYPT_TAG = new Tag(Tag.PREFIX + "crypt");
	public static final Pattern CRYPT_PATTERN = Pattern.compile("^CRYPT:([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$");
	public static final String CRYPT_FIRST = "C"; 
}
