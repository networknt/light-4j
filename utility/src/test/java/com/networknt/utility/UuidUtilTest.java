package com.networknt.utility;

import com.github.f4b6a3.uuid.UuidCreator;
import com.networknt.config.JsonMapper;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UuidUtilTest {
    // Regex to find 22-character Base64 URL-safe strings within single quotes
    // Assumes only A-Z, a-z, 0-9, _, - characters for the key
    private static final Pattern BASE64_SQL_KEY_PATTERN = Pattern.compile("'([A-Za-z0-9_-]{22})'");
    private static final Pattern BASE64_EVENT_KEY_PATTERN = Pattern.compile("\"([A-Za-z0-9_-]{22})\"");

    @Test
    public void testGetUUID() {
        UUID uuid = UuidUtil.getUUID();
        System.out.println(uuid);
        // Encode for URL
        String urlSafeId = UuidUtil.uuidToBase64(uuid);
        System.out.println("URL Safe ID:   " + urlSafeId); // e.g., AZZK6H62dLudJ60SgK7tTQ

        // Decode from URL
        UUID decodedUuid = UuidUtil.base64ToUuid(urlSafeId);
        System.out.println("Decoded UUID:  " + decodedUuid);

        System.out.println("Match: " + uuid.equals(decodedUuid));
        Assert.assertEquals(uuid, decodedUuid);
    }

    @Test
    @Ignore
    public void testSqlUuidConverter() {
        String inputFilename = "/home/steve/lightapi/portal-db/postgres/cfg.sql";
        String outputFilename = "/home/steve/lightapi/portal-db/postgres/output.sql";
        Path inputPath = Paths.get(inputFilename);
        Path outputPath = Paths.get(outputFilename);

        try {
            processSqlFile(inputPath, outputPath, BASE64_SQL_KEY_PATTERN);
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Test
    @Ignore
    public void testEventUuidConverter() {
        String inputFilename = "/home/steve/lightapi/event-importer/local.json";
        String outputFilename = "/home/steve/lightapi/event-importer/output.sql";
        Path inputPath = Paths.get(inputFilename);
        Path outputPath = Paths.get(outputFilename);

        try {
            processSqlFile(inputPath, outputPath, BASE64_EVENT_KEY_PATTERN);
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Test
    public void testUuidInMap() {
        Map<String, Object> map = new HashMap<>();
        UUID uuid = UuidUtil.getUUID();
        map.put("key", uuid);
        System.out.println("Map: " + map);
        String json = JsonMapper.toJson(map);
        System.out.println("Json: " + json);
        Map<String, Object> map2 = JsonMapper.string2Map(json);
        System.out.println("Map2: " + map2);
    }

    public static void processSqlFile(Path inputPath, Path outputPath, Pattern pattern) throws IOException {
        System.out.println("Processing " + inputPath + "...");

        // 1. Read the entire SQL script content
        String sqlContent = Files.readString(inputPath, StandardCharsets.UTF_8);

        // 2. Map to store original Base64 -> new UUIDv7 hex string
        Map<String, String> keyMap = new HashMap<>();
        long keysFound = 0;
        long uniqueKeysReplaced = 0;

        // 3. Use Matcher to find and replace
        Matcher matcher = pattern.matcher(sqlContent);
        // Use StringBuffer for efficient replacement building
        StringBuffer outputStringBuffer = new StringBuffer();

        while (matcher.find()) {
            keysFound++;
            String originalBase64Key = matcher.group(1); // Group 1 captures the part inside quotes

            String replacementUuidHex;
            if (keyMap.containsKey(originalBase64Key)) {
                // Reuse existing UUIDv7 if we've seen this key before
                replacementUuidHex = keyMap.get(originalBase64Key);
            } else {
                // Generate a new UUIDv7 and store the mapping
                UUID newUuidV7 = UuidCreator.getTimeOrderedEpoch(); // Generate UUIDv7
                replacementUuidHex = newUuidV7.toString(); // Get standard hex representation
                keyMap.put(originalBase64Key, replacementUuidHex);
                uniqueKeysReplaced++;
                // System.out.println("Mapping: '" + originalBase64Key + "' -> '" + replacementUuidHex + "'"); // Debugging
            }

            // Append the replacement (new UUIDv7 hex string, quoted)
            // Matcher.quoteReplacement handles any special characters ($) in the replacement
            matcher.appendReplacement(outputStringBuffer, Matcher.quoteReplacement("'" + replacementUuidHex + "'"));
        }

        // Append the rest of the file content that didn't match
        matcher.appendTail(outputStringBuffer);

        // 4. Write the modified content to the output file
        Files.writeString(outputPath, outputStringBuffer.toString(), StandardCharsets.UTF_8);

        System.out.println("Processed " + keysFound + " potential key occurrences.");
        System.out.println("Generated and mapped " + uniqueKeysReplaced + " unique UUIDv7 replacements.");
        System.out.println("Modified script saved to " + outputPath);
    }
}
