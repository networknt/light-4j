package com.networknt.config.yml;

import org.yaml.snakeyaml.nodes.Tag;

import java.util.regex.Pattern;

public class YmlConstants {
	public static final Tag CRYPT_TAG = new Tag(Tag.PREFIX + "crypt");
	public static final Pattern CRYPT_PATTERN = Pattern.compile("^CRYPT:.*$");
	public static final String CRYPT_FIRST = "C";
}
